import java.io.IOException;
import java.time.Clock;
import java.util.*;

class Scheduler {
    /*
    The scheduler for the operating system, starts a new timer task which stops the currently running process
    Repeats itself after stopping via the timer.schedule function (the second parameter having a new date will loop it
     */
    public void schedule() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    requestStop();//soft interrupt
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        };
        timer.schedule(task, new Date(), interrupt);//concurrently at repeatedly run the function at a rate of 250ms
    }

    public void CloseAllDevices() throws IOException {
        System.out.println("CLOSING DEVICES");
        // iterate through 10 vfs devices and close them, not a problem if the list isn't full
        for (int i = 0; i < 10; i++) {
            OS.kernel.close(i); // close can handle closing potentially unopened devices
        }
    }

    private final LinkedList<PCB> realtime = new LinkedList<>();
    private final LinkedList<PCB> interactive = new LinkedList<>();
    private final LinkedList<PCB> background = new LinkedList<>();
    private final LinkedList<PCB> sleep = new LinkedList<>();
    private final LinkedList<Long> sleepMillis = new LinkedList<>();
    static volatile OS.Priority currentPriority = OS.Priority.DEFAULT; // the current priority for the process running
    private final int interrupt = 250;
    private final Timer timer = new Timer();
    private Clock clock = Clock.systemUTC();
    UserlandProcess up;

    /*
    Creates a new process inside the scheduler, typically called via the kernel
     */
    public int CreateProcess(PCB up) throws InterruptedException {
        OS.pidMap.put(up.pid, up);
        if (up.priority == OS.Priority.DEFAULT || up.priority == OS.Priority.INTERACTIVE) {
            System.out.println("adding a default member");
            interactive.add(up);
        } else if (up.priority == OS.Priority.REALTIME) {
            realtime.add(up);
        } else if (up.priority == OS.Priority.BACKGROUND) {
            background.add(up);
        }
        //?up.getProcess().setThread(new Thread(up.getProcess()));
        return (int) up.getProcess().getID();
    }

    private LinkedList<PCB> getQueue(OS.Priority priority) {
        if (priority == OS.Priority.DEFAULT || priority == OS.Priority.INTERACTIVE) {
            return this.interactive;
        } else if (priority == OS.Priority.REALTIME) {
            return this.realtime;
        } else if (priority == OS.Priority.BACKGROUND) {
            return this.background;
        }
        return this.interactive;
    }

    /*
    Switching the process inside the scheduler,
    takes the head and puts it to the back of the list,
    then runs the new first
     */
    public void SwitchProcess() throws InterruptedException {
        OS.Priority previousPriority = currentPriority;
        OS.Priority chooseQueue = selectPriority();
        LinkedList<PCB> selectedQueue = getQueue(chooseQueue);
        LinkedList<PCB> lastQueue = getQueue(currentPriority);
        if (!selectedQueue.isEmpty()) currentPriority = chooseQueue;
        CreateFinishedSleepingProcesses();//every switch process, we check if sleep is done, and add back


        if (selectedQueue.isEmpty()) {
            if (lastQueue.isEmpty()) {
                selectedQueue = findNonEmptyQueue();
            } else
                selectedQueue = lastQueue;
        }
        if (selectedQueue.size() > 1) { // if the size were 1, it would be redundant to move the first process to the back
            selectedQueue.add(selectedQueue.removeFirst());
        }
        if (hasCurrentlyRunning() && currentlyRunning().getProcess().isRunning())
            selectedQueue.getFirst().run();
        else if (hasCurrentlyRunning()) currentlyRunning().getProcess().getThread().start();
    }

    public int getRandom(int max) {
        int random = (int) (Math.random() * max) + 1;
        return random;
    }

    private OS.Priority selectPriority() {
        int random = getRandom(10);
        if (random <= 6) return OS.Priority.REALTIME;
        else if (random <= 9) return OS.Priority.INTERACTIVE;
        else return OS.Priority.BACKGROUND;
    }

    private LinkedList<PCB> findNonEmptyQueue() {
        if (!realtime.isEmpty()) {
            currentPriority = OS.Priority.REALTIME;
            return realtime;
        }
        if (!interactive.isEmpty()) {
            currentPriority = OS.Priority.INTERACTIVE;
            return interactive;
        }
        if (!background.isEmpty()) {
            currentPriority = OS.Priority.BACKGROUND;
            return background;
        }
        return realtime;
    }

    /*
    Since the currently running process is the head of the list, we can return the head
     */
    public PCB currentlyRunning() {
        if (getQueue(currentPriority).isEmpty()) {
            System.out.println(interactive + "\n" + background + "\n" + realtime);
        }
        return getQueue(currentPriority).getFirst();
    }

    /*
    Since our process cooperate()'s, we can simply request our process to stop, which it will when it cooperates
    requestStop sets our expired flag to true, which will SwitchProcess inside cooperate
     */
    public void requestStop() throws InterruptedException {
        if (getQueue(currentPriority).isEmpty() || getQueue(currentPriority).getFirst().getProcess().getExpired()) {
            return;
        }
        if (!interactive.isEmpty()) interactive.getFirst().getProcess().requestStop();
        if (!realtime.isEmpty()) realtime.getFirst().getProcess().requestStop();
        if (!background.isEmpty()) background.getFirst().getProcess().requestStop();
        currentlyRunning().getProcess().requestStop();
    }

    /*
    scheduler sleep, modifies two main queues holding the process and millisecond time value, switches the process after
     */
    public void sleep(int milliseconds) throws InterruptedException {
        if (getQueue(currentPriority).isEmpty()) return;
        long currentMillis = clock.millis();
        long time = currentMillis + milliseconds;
        //add the currently running to the sleep queue
        System.out.println(currentPriority + " IS BEING REMOVED FROM THE QUEUE");
        sleep.add(getQueue(currentPriority).removeFirst());
        OS.pidMap.remove(sleep.getLast());
        sleepMillis.add(time);
        System.out.println(sleep.getLast() + " IS GOING TO SLEEP FOR " + milliseconds + " MILLISECONDS");
        System.out.println("YOU WILL NOT SEE " + sleep.getLast() + " FOR ANOTHER " + milliseconds + " MILLISECONDS");
        //OS.SwitchProcess();
        SwitchProcess();
    }

    /*
    check to ensure the first position of the queue is ready to be taken off
     */
    private boolean SleepingProcessIsFinished() {
        //ensure the list isn't empty before checking
        return !sleepMillis.isEmpty() && sleepMillis.getFirst() < clock.millis();
    }

    public void deSchedule(int pid) {
        if (!background.isEmpty()) {
            for (int i = 0; i < background.size(); i++) {
                if (background.get(i).pid == pid) {
                    background.remove(i);
                }
            }
        }
        if (!interactive.isEmpty()) {
            for (int i = 0; i < interactive.size(); i++) {
                if (interactive.get(i).pid == pid) {
                    interactive.remove(i);
                }
            }
        }
        if (!realtime.isEmpty()) {
            for (int i = 0; i < realtime.size(); i++) {
                if (realtime.get(i).pid == pid) {
                    System.out.println("REMOVING PID: " + pid);
                    realtime.remove(i);
                }
            }
        }
    }

    /*
    if the first in line is ready to be taken off, we take it off and store it in its priority destined queue
     */
    private void CreateFinishedSleepingProcesses() throws InterruptedException {
        while (SleepingProcessIsFinished()) {
            //check to see if there is a finished sleeping process, remove all that are done
            if (sleep.getFirst().timeoutCount++ > 4)
                demote(sleep.getFirst());//check to see if the timeout exceeds 5, if so demote
            else
                CreateProcess(sleep.removeFirst());
            sleepMillis.removeFirst();
        }
    }

    /*
    if the list is not empty, then there should be a process running
     */
    public boolean hasCurrentlyRunning() {
        return !getQueue(currentPriority).isEmpty();
    }

    /*
    demote a given process by changing the priority member inside the PCB class
     */
    public void demote(PCB process) {
        process.timeoutCount = 0;
        System.out.println("DEMOTING " + process + " WITH PRIORITY " + process.priority);
        if (process.priority == OS.Priority.REALTIME) {
            //add to queue.
            interactive.add(process);
            process.priority = OS.Priority.INTERACTIVE;
            if (realtime.isEmpty()) currentPriority = OS.Priority.INTERACTIVE;
        } else {
            //add to queue
            background.add(process);
            process.priority = OS.Priority.BACKGROUND;
            if (interactive.isEmpty()) currentPriority = OS.Priority.BACKGROUND;
        }
    }

    /*
    promote a given process by changing the priority member inside the PCB class
    not used for our assignment yet
     */
    public void promote(PCB process) {
        if (process.priority == OS.Priority.BACKGROUND) {
            //when we promote, we add the first priority from the process queue that the given process is in.
            getQueue(OS.Priority.INTERACTIVE).add(getQueue(process.priority).removeFirst());
            process.priority = OS.Priority.INTERACTIVE;
            //add to queue.
        } else {
            getQueue(OS.Priority.REALTIME).add(getQueue(process.priority).removeFirst());
            process.priority = OS.Priority.REALTIME;
            //add to queue.
        }
    }

    /* return current running process pid */
    int GetPid() {
        return currentlyRunning().pid;
    }

    /* Iterate over the PID map, find a PID with the process name equal to the param name */
    int GetPidByName(String name) {
        for (Map.Entry<Integer, PCB> map : OS.pidMap.entrySet()) { // iterate over map entries
            if (map.getValue().name.equals(name)) {
                return map.getKey();
            }
        }
        return -1; // name not found
    }

    public PCB getRandomProcess() {
        int rand = getRandom(2);
        if (rand == 0) {
            // background
            if (!background.isEmpty()) {
                return background.get(getRandom(background.size() - 1));
            }
            rand = 1; // set to next priority (2/3 random)
        }
        if (rand == 1) {
            if (!realtime.isEmpty()) {
                return realtime.get(getRandom(realtime.size() - 1));
            }
        }
        if (!interactive.isEmpty()) {
            return interactive.get(getRandom(realtime.size() - 1));
        }
        return null; // no processes running
    }
}