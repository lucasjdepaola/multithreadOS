import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;

class HelloWorld extends UserlandProcess {
    public HelloWorld(Thread thread, Semaphore semaphore) {
        super(thread, semaphore);
    }

    void main() throws InterruptedException, IOException {
//        int open = Open("file lucasdepaola.txt");
//        byte[] writeBytes = {1, 2, 3, 4, 5};
//        //Seek(open, writeBytes.length);
//        Write(open, writeBytes);
//        Seek(open, 0);
//        Read(open, writeBytes.length);
//        int open1 = Open("file test.dat");
//        System.out.println(open1 + " the second open should be 1");
//        byte[] secondBytes = "writing to the second file".getBytes(StandardCharsets.UTF_8);
//        Seek(open1, writeBytes.length);
//        Write(open1, secondBytes);
//        Seek(open1, writeBytes.length);
//        Read(open1, secondBytes.length); // this should read "writing to the second file" in the byte equivalent
//        byte[] thirdBytes = "writing some more data to the vfs".getBytes(StandardCharsets.UTF_8);
//        int open2 = Open("file moredata.dat");
//        Write(open2, thirdBytes);
//        Seek(open2, 0);
//        Read(open2, thirdBytes.length);

        while (true) {
            System.out.println("hello world!");
            Thread.sleep(50); // sleeping per iteration to see it cooperating properly

            int ptr2 = allocateMemory(2048);
            if(ptr2 != -1) {
                for(int i = 0; i < 2048; i++) {
                    byte b = (byte)i; // this cast won't be 1:1, but as long as the read and write values are the same, it shouldn't matter
                    System.out.println("writing byte " + b + ", to virtual address " + i);
                    Write(i, b);
                    System.out.println("reading byte " + b + " from address " + i);
                    byte r = Read(i);
                    System.out.println("read value is : " + r);

                }
                free(ptr2, 2048);
            }
            cooperate();
        }
    }

    public String toString() {
        return "HelloWorld";
    }
}