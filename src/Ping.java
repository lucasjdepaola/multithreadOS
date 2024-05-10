import java.util.concurrent.Semaphore;

class Ping extends UserlandProcess {
    public Ping(Thread thread, Semaphore semaphore) {
        super(thread, semaphore);
    }

    void main() throws InterruptedException {
        int inc = 0;
        while(true) {
//            SendMessage("PING", "Pong", inc++); // sending ping to pong
//            KernelMessage km = WaitForMessage();
//            if(km != null) {
//                System.out.println(km + ", inside the ping process");
//            }
            System.out.println("IDLE process");
            Thread.sleep(300);
            cooperate();
        }
    }

    public String toString() {
        return "Ping";
    }
}