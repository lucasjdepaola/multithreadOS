import java.util.concurrent.Semaphore;

class Wave extends UserlandProcess {
    public Wave(Thread thread, Semaphore semaphore) {
        super(thread, semaphore);
    }

    /*
    Create a wave pattern printed on the terminal, Thread.sleep's per "/" print statement for a better visual
    At the end, it will 'scheduler.sleep' for 5 seconds, letting other processes run repeatedly, then it will come back
    for another iteration.
     */
    @Override
    public void main() throws InterruptedException {
        while (true) {
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < i; j++) {
                    System.out.printf(" ");
                }
                System.out.println("\\");
                Thread.sleep(20);
                if(i == 9) System.out.printf("          |\n          |\n");
            }
            for (int i = 10; i >= 0; i--) {
                for (int j = 0; j < i; j++) {
                    System.out.printf(" ");
                }
                System.out.println("/");
                Thread.sleep(20);
            }
            OS.sleep(5000);
            /*
            This puts the process away from the scheduler for 5 seconds, ideally it should iterate once, not show up
            for another 5 seconds while other processes are running, then come back to the scheduler with its priority
            intact, then repeat.
             */

            cooperate();
        }
    }

    @Override
    public String toString() {
        return "Wave";
    }
}