import java.io.IOException;
import java.util.concurrent.Semaphore;

class GoodbyeWorld extends UserlandProcess {
    public GoodbyeWorld(Thread thread, Semaphore semaphore) {
        super(thread, semaphore);
    }

    void main() throws InterruptedException, IOException {
//        int open = Open("random 289347");
//        Read(open, 4);
        while (true) {
            System.out.println("goodbye world");
            Thread.sleep(50); // sleeping per iteration to see it cooperating properly
//            int openfile = Open("file lucasdepaola.txt");
//            Seek(openfile, 0);
//            Read(openfile, 5); // should be 1, 2, 3, 4, 5 which has been written by the HelloWorld process
//            int rand = Open("random 1234"); // testing to see if it overflows
//            System.out.println("infinite loop of open('random') returned: " + rand); // this should eventually return -1 after hitting all 10 spots in memory
//            if (rand != -1) Read(rand, 4); // reading some random bytes
//            else {
//                System.out.println("VFS IS FULL, CLOSING ALL DEVICES"); // close and continue opening random spots
//                closeAll();
//            }
            int ptr = allocateMemory(1024);
            if(ptr != -1) {
                for(int i = 0; i < 1024; i++) {
                    System.out.println("writing # to address " + i);
                    Write(i, (byte)'#');
                    System.out.println("reading # value from address " + i);
                    System.out.println(Read(i));
                }
                free(ptr, 1024);
            }
            int secondPtr = allocateMemory(1024);
            if(secondPtr != -1) {
                for(int i = 0; i < 1024; i++) {
                    System.out.println("writing byte " + (byte)i + " to address " + i);
                    byte two = 2;
                    Write(i, (byte)i);
                    System.out.println("reading address" + i);
                    System.out.println(Read(i) + ", is the value read");
                }
                free(secondPtr, 1024);
            }
            cooperate();
        }
    }

    public String toString() {
        return "GoodbyeWorld";
    }
}