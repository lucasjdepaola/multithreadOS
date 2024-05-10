import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

class OS {
    static UserlandProcess up;
    static Thread thread;
    public static final int permits = 0;
    static Semaphore semaphore;

    enum CallType {
        CreateProcess, SwitchProcess, SLEEP, KERNELMESSAGESEND
    }

    enum Priority {
        REALTIME, INTERACTIVE, BACKGROUND, DEFAULT
    }


    static HashMap<Integer, PCB> pidMap = new HashMap<Integer, PCB>();
    static HashMap<Integer, PCB> messageWaitMap = new HashMap<>();
    static CallType currentCall;
    static Kernel kernel = initKernel();

    private static Kernel initKernel() {
        Kernel kernel = new Kernel(thread, new Semaphore(OS.permits));
        kernel.thread = new Thread(kernel);//instantiate new thread with self object
        return kernel;
    }

    static volatile ArrayList<Object> list = new ArrayList<>();


    /*
    Turns the userland process into a PCB, assigns its priority when given in the parameters
     */
    public static int CreateProcess(UserlandProcess up, Priority priority) throws InterruptedException {
        PCB process = new PCB(up, priority);
        System.out.println("creating process");
        list.add(process);
        currentCall = CallType.CreateProcess;
        kernel.start();
        return (int) up.getID();
    }

    /*
    Default create process method, turns userland process into a PCB with default interactive priority
     */
    public static int CreateProcess(UserlandProcess up) throws InterruptedException {
        PCB upPCB = new PCB(up, Priority.DEFAULT);
        pidMap.put(upPCB.pid, upPCB);
        list.add(upPCB);//add process to the OS list
        currentCall = CallType.CreateProcess;
        kernel.start();
        //OS goes into the create process state, so when the kernel runs, it knows to create processes from the OS
        return (int) up.getID();
    }

    public static void SwitchProcess() throws InterruptedException {
        if (currentCall != CallType.SLEEP)//&& currentCall != CallType.KERNELMESSAGESEND
            currentCall = CallType.SwitchProcess;
        //OS goes into the switch process state, so it knows to switch processes, then runs the kernel
        kernel.start();
    }

    public static void Startup(UserlandProcess init) throws InterruptedException {
        semaphore.release(); //acquire the OS semaphore
        IdleProcess idle = new IdleProcess(thread, new Semaphore(OS.permits));
        idle.setThread(new Thread(idle));
        CreateProcess(idle, Priority.BACKGROUND);
        CreateProcess(init, Priority.REALTIME);
        kernel.getScheduler().schedule();//initialize the scheduler for interrupts
        kernel.thread.start();// start the kernel
        semaphore.acquire();
    }

    /*
    sleeps for a designated amount of time, calls the kernel sleep version
     */
    public static void sleep(int milliseconds) throws InterruptedException {
        OS.currentCall = CallType.SLEEP;
        list.addFirst(milliseconds);
    }

    /* call kernel method */
    public static int GetPid() {
        return kernel.GetPid();
    }

    /* call kernel method */
    public static int GetPidByName(String name) {
        return kernel.GetPidByName(name);
    }

    /* call kernel method */
    public static void SendMessage(KernelMessage km) throws InterruptedException {
        kernel.sendMessage(km);
    }

    /* call kernel method */
    public static KernelMessage WaitForMessage() throws InterruptedException {
        return kernel.waitForMessage();
    }


    public static void GetMapping(int virtualPageNumber) throws IOException {
        kernel.GetMapping(virtualPageNumber);
    }

    public static int allocMemory(int size) {
        if(size % 1024 != 0)
            return -1; // ensuring size is a multiple of 1024
        return kernel.allocMemory(size);
    }

    public static boolean free(int pointer, int size) {
        System.out.println("FREEING MEMORY OF SIZE " + size);
        if(pointer % 1024 != 0 || size % 1024 != 0)
            return false; // ensuring size and pointer are multiples of 1024
        /* call kernel method */
        return kernel.free(pointer, size);
    }
}