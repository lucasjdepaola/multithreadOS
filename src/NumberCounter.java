import java.util.concurrent.Semaphore;

/*
Process to show that the SwitchProcess does not reset the currently looping process
the counting variable will show this
 */
class NumberCounter extends UserlandProcess {

    public NumberCounter(Thread thread, Semaphore semaphore) {
        super(thread, semaphore);
    }

    @Override
    void main() throws InterruptedException {
        int i = 0;
        while(true) {
            System.out.println("the counting number is: " + i++);
            Thread.sleep(50);
            cooperate();
        }
    }

    @Override
    public String toString() {
        return "NumberCounter";
    }
}