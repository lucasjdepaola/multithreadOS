import java.io.IOException;
import java.util.concurrent.Semaphore;

public class Main {
    public static void main(String[] args) throws Exception {
        final String version = System.getProperty("java.version");
        final int versionInt = Integer.parseInt(version.substring(0, version.indexOf('.')));
        System.out.println("Using java version: " + versionInt);
        if(versionInt < 21) {
            /*
            thread.getID() does not work on java version 21, which this project is based on.
            using a version less than 21 will potentially throw errors that do not happen on version 21.
            */
            throw new Exception("WARNING:lst You are using a java version lower than 21 which the program was written on\nif you want to continue with unexpected and unintended behavior, delete main:14\n- Lucas DePaola");
        }
        Semaphore semaphore = new Semaphore(OS.permits);
        UserlandProcess Ping = new Ping(new Thread(), semaphore);
        Ping.setThread(new Thread(Ping));
        OS.thread = new Thread();
        OS.semaphore = new Semaphore(0);

        UserlandProcess Pong = new Pong(new Thread(), new Semaphore(OS.permits));
        Pong.setThread(new Thread(Pong));
        OS.Startup(Pong); //start the O
        OS.CreateProcess(Ping, OS.Priority.REALTIME);
        UserlandProcess helloworld = new HelloWorld(new Thread(), new Semaphore(OS.permits));
        UserlandProcess goodbyeworld = new GoodbyeWorld(new Thread(), new Semaphore(OS.permits));
        helloworld.setThread(new Thread(helloworld));
        goodbyeworld.setThread(new Thread(goodbyeworld));
        OS.CreateProcess(helloworld);
        OS.CreateProcess(goodbyeworld);

        //counting an integer to show the thread retains memory after switching process
//        NumberCounter counter = new NumberCounter(new Thread(), new Semaphore(OS.permits));
//        counter.setThread(new Thread(counter));
//        OS.CreateProcess(counter);
//
//        //process to show OS.sleep functionality while also being the highest priority
//        Wave wave = new Wave(new Thread(), new Semaphore(OS.permits));
//        wave.setThread(new Thread(wave));
//        OS.CreateProcess(wave, OS.Priority.REALTIME);
    }
}