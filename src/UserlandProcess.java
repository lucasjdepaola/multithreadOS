import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;

abstract class UserlandProcess implements Runnable {
    public UserlandProcess(Thread thread, Semaphore semaphore) {
        this.thread = thread;
        this.semaphore = semaphore;
    }

    private Thread thread;
    private Semaphore semaphore;
    private boolean hasRan = false;
    public static byte[] memory = new byte[1024 * 1024];
    public static int[][] TLB = new int[2][2];

    public static void clearTLB() {
        for(int i = 0; i < TLB.length; i++) {
            for(int j = 0; j < TLB[i].length; j++) {
                TLB[i][j] = -1;
            }
        }
        System.out.println("###### TLB CLEARED ######");
    }

    public Thread getThread() {
        return thread;
    }

    public void setThread(Thread thread) {
        this.thread = thread;
    }

    public long getID() {
        // TODO this is not an error, this is the higher version of java's way to retrieve a threadID, the lower version is deprecated for me
        return thread.threadId();
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    private boolean isExpired = false;

    boolean getExpired() {
        return isExpired;
    }

    void requestStop() throws InterruptedException {
        isExpired = true;
    }

    abstract void main() throws InterruptedException, IOException;

    boolean isStopped() {
        return semaphore.availablePermits() == 0;
    }

    boolean isDone() {
        return !thread.isAlive();
    }

    void start() throws InterruptedException {
//        if (semaphore.availablePermits() > 1) new Exception().printStackTrace();
        semaphore.drainPermits();
        semaphore.release();
    }

    void stop() throws InterruptedException {
//        if(semaphore.availablePermits() == 0) new Exception().printStackTrace();
        semaphore.acquire();
    }

    public void run() {
        hasRan = true;
        try {
            main();
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    void cooperate() throws InterruptedException {
        start();
        if (isExpired) {
            isExpired = false;
            clearTLB();
            semaphore.drainPermits();
            OS.SwitchProcess();
        }
        stop();
    }

    boolean isRunning() {
        return hasRan;
    }

    int Open(String s) throws IOException {
        return OS.kernel.Open(s);
    }

    byte[] Read(int id, int size) throws IOException {
        return OS.kernel.Read(id, size);
    }

    void Write(int id, byte[] data) throws IOException {
        OS.kernel.Write(id, data);
    }

    void Seek(int id, int to) throws IOException {
        OS.kernel.Seek(id, to);
    }

    void close(int id) throws IOException {
        OS.kernel.close(id);
    }

    void closeAll() throws IOException {
        OS.kernel.closeAll();
    }

    void SendMessage(String message, String processTo, int what) throws InterruptedException {
//        KernelMessage km = new KernelMessage(message.getBytes(StandardCharsets.UTF_8), (int)getID(), 1, 1, OS.GetPidByName(processTo));
        KernelMessage km = new KernelMessage(message.getBytes(StandardCharsets.UTF_8), OS.GetPidByName(processTo), 1, what, (int) getID());
        OS.SendMessage(km);
    }

    KernelMessage WaitForMessage() throws InterruptedException {
        return OS.WaitForMessage();
    }


    byte Read(int address) throws IOException {
        int virtualPageNumber = address / 1024;
        int pageoffset = address % 1024;
        OS.kernel.lazyAlloc(address); // lazy alloc
        //next look at the TLB to see vpage -> phys mapping is there
        int coord = TLBCoord(virtualPageNumber);
        if(coord != -1) {
            int physaddress = TLB[coord][1] * 1024 + pageoffset;
            System.out.println(physaddress + ", " + TLB[coord][1]);
            return memory[physaddress];
        } else {
            GetMapping(virtualPageNumber);
            coord = TLBCoord(virtualPageNumber);
            if(coord == -1) {
                System.out.println("err");
//                thread.interrupt(); // kill process
                return -1;
            }
            return memory[TLB[coord][1] * 1024 + pageoffset]; // try again
        }
    }

    void Write(int address, byte value) throws IOException {
        //address / pagesize to get the page number
        OS.kernel.lazyAlloc(address); // lazy alloc
        int virtualPageNumber = address / 1024;
        int pageoffset = address % 1024;
        System.out.println("pageoffset is : " + pageoffset);
        int coord = TLBCoord(virtualPageNumber);
        if(coord != -1) {
            int physaddress = TLB[coord][1] * 1024 + pageoffset;
            System.out.println("physaddress is " + physaddress);
            System.out.println("writing value " + value + " to address " + address);
            memory[physaddress] = value;
        } else {
            GetMapping(virtualPageNumber);
            coord = TLBCoord(virtualPageNumber);
            System.out.println("writing again using GetMapping");
            if(coord != -1 && TLB[coord][1] != -1) {
                System.out.println(coord + ", " + TLB[coord][1] + ", " + pageoffset);
                memory[TLB[coord][1] * 1024 + pageoffset] = value;
            } else {
                // throw err
            }
        }
    }

    public static void GetMapping(int virtualPageNumber) throws IOException {
        OS.GetMapping(virtualPageNumber);
    }

    private int TLBCoord(int virtualPageNumber) {
        // tlb coord finds the physpage of the coords
        for(int i = 0; i < TLB.length; i++) {
            if(TLB[i][0] == virtualPageNumber) {
                return i; // we can now get the coords by doing TLB[coord][1]
            }
        }
        return -1;
    }

    public int allocateMemory(int size) {
        return OS.allocMemory(size);
    }

    public boolean free(int pointer, int size) {
        return OS.free(pointer, size);
    }

    public int freeAll() {
        return -1;
    }

    public abstract String toString();

}