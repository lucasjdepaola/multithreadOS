import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.Semaphore;

class Kernel implements Runnable, Device {
    public Kernel(Thread thread, Semaphore semaphore) {
        this.thread = thread;
        this.semaphore = semaphore;
        Arrays.fill(pageIsFree, true); // all start as free until filled
        swapFile = new FakeFileSystem();
    }

    Thread thread;
    Semaphore semaphore;
    FakeFileSystem swapFile;
    private final Scheduler scheduler = new Scheduler();
    boolean[] pageIsFree = new boolean[1024];

    public Scheduler getScheduler() {
        return scheduler;
    }


    void start() {
        semaphore.release();
    }

    void stop() throws InterruptedException {
        semaphore.acquire();
    }

    /*
    runs the kernel similar to a state machine, per allowed iteration, the kernel will either create or switch a process
    while also running the current process. We use start() because run() calls the main method, which would restart
    the loop
     */
    public void run() {
        while (true) {
            start();
            OS.CallType currentCall = OS.currentCall;
            switch (currentCall) {
                case OS.CallType.CreateProcess:
                    try {
                        CreateProcess((PCB) OS.list.removeFirst());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    if (OS.list.isEmpty())
                        OS.currentCall = OS.CallType.SwitchProcess;
                    break;
                case OS.CallType.SwitchProcess:
                    try {
                        switchProcess();
                        semaphore.drainPermits();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case OS.CallType.SLEEP:
                    System.out.println("sleeping");
                    try {
                        sleep((int) OS.list.removeFirst());
                        semaphore.drainPermits();
                        //take the parameters off of the OS queue which should indicate the time in milliseconds
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case OS.CallType.KERNELMESSAGESEND:
                    try {
                        sendMessage((KernelMessage) OS.list.removeFirst());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    semaphore.drainPermits();
                    break;
//                case OS.CallType.KERNELMESSAGEWAIT:
//                    waitForMessage();
//                    break;
            }
            try {
                stop(); // acquire a semaphore with zero permits, which makes the kernel hang until prompted again
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private void CreateProcess(PCB up) throws InterruptedException {
        // call the schedulers version
        scheduler.CreateProcess(up);
    }

    private void switchProcess() throws InterruptedException {
        // call the schedulers version
        scheduler.SwitchProcess();
    }

    /*
    sleeps for a designated amount of time, calls the schedulers version.
     */
    public void sleep(int milliseconds) throws InterruptedException {
        OS.currentCall = OS.CallType.SwitchProcess;
        scheduler.sleep(milliseconds);
    }

    private final VFS vfs = new VFS();

    PCB getCurrentlyRunning() {
        return scheduler.currentlyRunning();
    }


    @Override
    public int Open(String s) throws IOException {
        System.out.println("Open device call inside the kernel.");
        // iterate over 10 vfs devices, if -1, store open on index
        for (int i = 0; i < 10; i++) {
            if (getCurrentlyRunning().ids[i] == -1) {
                int open = vfs.Open(s);
                getCurrentlyRunning().ids[i] = open;
                System.out.println("Successfully opened on index " + i);
                System.out.println("vfs open ID: " + open);
                if (open == -1) {
                    System.out.println("VFS list is full, cannot open");
                    return -1;
                }
                return i;
            }
        }
        return -1;
    }

    @Override
    public void close(int id) throws IOException {
        if (id == -1)
            return; // since we're using an initialized with -1 array of ids, we could potentially try to write to -1
        vfs.close(getCurrentlyRunning().ids[id]);
        getCurrentlyRunning().ids[id] = -1;
    }

    @Override
    public byte[] Read(int id, int size) throws IOException {
        if (id == -1)
            return null; // since we're using an initialized with -1 array of ids, we could potentially try to write to -1
        return vfs.Read(getCurrentlyRunning().ids[id], size);
    }

    @Override
    public void Seek(int id, int to) throws IOException {
        if (id == -1)
            return;// since we're using an initialized with -1 array of ids, we could potentially try to write to -1
        vfs.Seek(getCurrentlyRunning().ids[id], to);
    }

    @Override
    public int Write(int id, byte[] data) throws IOException {
        if (id == -1)
            return -1; // since we're using an initialized with -1 array of ids, we could potentially try to write to -1
        System.out.println("Attempting to write via a kernel call on index " + id);
        return vfs.Write(id, data);
    }

    public void closeAll() throws IOException {
        // call the scheduler
        scheduler.CloseAllDevices();
    }

    int GetPid() {
        return scheduler.GetPid();
    }

    int GetPidByName(String name) {
        return scheduler.GetPidByName(name);
    }

    void sendMessage(KernelMessage km) throws InterruptedException {
        KernelMessage kernelMessage = km.copy(); // copy kernel message
        /* find sender and receiver using pidmap */
        PCB receiver = OS.pidMap.get(kernelMessage.receiverpid);
        PCB sender = OS.pidMap.get(GetPid());

        if (OS.messageWaitMap.containsKey(receiver.pid)) {
            receiver.UnWaitProcess(); // unwait process if the receiver is already waiting and receives a message
        }
        receiver.messages.add(kernelMessage); // add km to the queue
    }

    /*
    if the process has a message, return it, otherwise add it to the waiting map
     */
    KernelMessage waitForMessage() throws InterruptedException {
        if (!getCurrentlyRunning().messages.isEmpty()) { // if there is an existing message, return it
            KernelMessage km = getCurrentlyRunning().messages.removeFirst();
            if (OS.messageWaitMap.containsKey(GetPid())) {
                OS.messageWaitMap.remove(GetPid()).UnWaitProcess(); // remove from the wait map and set wait state to false
            }
            return km;
        }
        /* if the process wants to wait for a message without any messages */
        if (!OS.messageWaitMap.containsKey(GetPid())) {
            OS.messageWaitMap.put(GetPid(), getCurrentlyRunning());
            getCurrentlyRunning().WaitProcess();
        }
        return null; // there is no return value for a process that waits with no intended message
    }

    int pageNumber = 0;

    /* updated getMapping, steal a page and write to swapfile if there are no pages available */
    public void GetMapping(int virtualPageNumber) throws IOException {
        /*
        {[virtual, physical],
        [virtual, physical]}
         */
        if (getCurrentlyRunning().pageTable[virtualPageNumber].physicalPageNumber == -1) {
            // find physical page in inuse array and assign it
            // if there is none, page swap
            for (int i = 0; i < pageIsFree.length; i++) {
                if (pageIsFree[i]) {
                    pageIsFree[i] = false; // find physical page in inuse array and assign it
                    getCurrentlyRunning().pageTable[i / 1024].physicalPageNumber = i;
                    //update tlb
                    int rand = scheduler.getRandom(1);
                    UserlandProcess.TLB[rand][0] = i; // update virtual address
                    UserlandProcess.TLB[rand][1] = 100;// change
                    return;
                }
            }
            // else pageswap
            PCB randomProcess = getRandomProcess();
            for (int i = 0; i < randomProcess.pageTable.length; i++) {
                if (randomProcess.pageTable[i] != null && randomProcess.pageTable[i].physicalPageNumber != -1) {
                    int id = 0; // foo id
                    if (randomProcess.pageTable[i].diskpagenumber != -1) {
                        // this means that it has written data already in the swap file
                        // dirty page
                        swapFile.Seek(id, randomProcess.pageTable[i].diskpagenumber * 1024); // offset 1024
                        byte[] bytes = swapFile.Read(id, 1024); // read the file
                        for (int j = 0; i < bytes.length; j++) {
                            UserlandProcess.memory[j + randomProcess.pageTable[i].physicalPageNumber] = bytes[i];
                            // write back to page
                        }
                        return;
                    }
                    randomProcess.pageTable[i].diskpagenumber = pageNumber++;
                    // now write to ffs
                    swapFile.Seek(id, pageNumber * 1024); // offset 1024
                    byte[] bytes = new byte[1024];
                    // fill bytes
                    for (int j = 0; j < 1024; j++) {
                        bytes[i] = randomProcess.getProcess().Read(i);
                    }
                    System.out.println("WRITING OUT PAGE");
                    swapFile.Write(id, bytes); // write page
                }
            }
        }
        int randInt = scheduler.getRandom(1);
        UserlandProcess.TLB[randInt][0] = virtualPageNumber;
        if (UserlandProcess.TLB[randInt][1] == -1) UserlandProcess.TLB[randInt][1] = virtualPageNumber;
    }

    private PCB getRandomProcess() {
        return scheduler.getRandomProcess();
    }

    public boolean free(int pointer, int size) {
        for (int i = pointer; i < (size + pointer) / 1024; i++) {
            pageIsFree[i] = true; // mark physical space free
            getCurrentlyRunning().pageTable[pointer / 1024] = null; // clear virtual to physical mapping
        }
        return true;
    }

    /* checks to see if it can allocate, then lazy allocates */
    public int allocMemory(int size) {
        for (int i = 0; i < pageIsFree.length; i += 1024) {
            if (canAlloc(i, size)) {
                System.out.println("lazy allocated " + i);
                return i;
            }
        }
        return -1;
    }

    private boolean canAlloc(int start, int size) {
        for (int i = start / 1024; i < (size / 1024) + (start / 1024); i++)
            if (!pageIsFree[i]) return false;
        return true;
    }

    /* only runs before real usage, we'll know that we are clear to allocate, however lazyAlloc will allocate per usage */
    int lazyAlloc(int start) {
        System.out.println(start);
        pageIsFree[start] = false;
        getCurrentlyRunning().pageTable[start/1024] = new VirtualToPhysicalMapping();
        getCurrentlyRunning().pageTable[start/1024].physicalPageNumber = start*1024;
        return start;
    }
}