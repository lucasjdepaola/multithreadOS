import java.util.Arrays;
import java.util.LinkedList;

class PCB {
    private UserlandProcess up;

    public UserlandProcess getProcess() {
        return up;
    }

    static int nextPid;
    int pid;
    int timeoutCount = 0;
    public int[] ids = initIDs();
    String name;
    boolean isWaiting = false;

//    int[] pageTable = new int[100]; // virtual page number is index to the array, phys page number is the value
    VirtualToPhysicalMapping[] pageTable = new VirtualToPhysicalMapping[100]; // virtual page number is index to the array, phys page number is the value

    /*
    if a process is waited, it cannot run in the run() method
     */
    void WaitProcess() {
        isWaiting = true;
    }

    void UnWaitProcess() {
        isWaiting = false;
    }

    LinkedList<KernelMessage> messages = new LinkedList<>();

    private int[] initIDs() {
        int[] ids = new int[10];
        for (int i = 0; i < 10; i++) {
            ids[i] = -1;
        }
        return ids;
    }

    public OS.Priority priority;

    public PCB(UserlandProcess up, OS.Priority priority) {
        this.up = up;
        this.priority = priority;
        this.pid = (int) up.getID();
        this.name = getName();
//        Arrays.fill(pageTable, -1);
    }

    void stop() throws InterruptedException {
        while (!up.isStopped()) {
            up.stop();
            Thread.sleep(50);
        }
    }

    boolean isDone() {
        return up.isDone();
    }

    void run() throws InterruptedException {
        if (isWaiting) return; // for the message state
        up.start();
    }

    private String getName() {
        if (up == null) return "null";
        return up.getClass().getSimpleName();
    }

    public String toString() {
        return up.toString();
    }

}