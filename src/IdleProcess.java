import java.util.concurrent.Semaphore;

class IdleProcess extends UserlandProcess {

    public IdleProcess(Thread thread, Semaphore semaphore) {
        super(thread, semaphore);
    }

    /*
    this process is intended to slow down the output, and sleep the entire OS for a certain amount of time.
    */
    void main() throws InterruptedException {
        while (true) {
            System.out.println("Idle process, sleeping 500 milliseconds");
            Thread.sleep(500);
            cooperate();
            //stop();
        }
    }

    public String toString() {
        return "idle";
    }
}