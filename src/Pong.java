import java.util.concurrent.Semaphore;

class Pong extends UserlandProcess {
    public Pong(Thread thread, Semaphore semaphore) {
        super(thread, semaphore);
    }

    void main() throws InterruptedException {
        int inc = 0;
        while(true) {
//            KernelMessage km = WaitForMessage();
//            if(km != null) {
//                System.out.println(km + ", inside the pong process");
//                SendMessage("PONG", "Ping", inc++);
//            }
//            // might not need to use waitformessage inside the userland process
            System.out.println("IDLE process");
            Thread.sleep(300);
            cooperate();
        }
    }

    public String toString() {
        return "Pong";
    }
}
