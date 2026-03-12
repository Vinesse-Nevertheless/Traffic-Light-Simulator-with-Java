package traffic;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

class SystemRunnable implements Runnable {

    long systemStartTimeInMillis;
    long timeRunningInSecs = 0;

    volatile boolean isInSystemState = false;
    volatile boolean isActive = true;

    AtomicReferenceArray<String[]> roadQueue;

    AtomicInteger enqueuePointer;
    AtomicInteger dequeuePointer;
    AtomicInteger availableSlotCount;
    AtomicInteger intervalDigit = new AtomicInteger(0);
    AtomicInteger openPointer = new AtomicInteger(0);
    AtomicInteger nextToOpenPointer = new AtomicInteger(openPointer.intValue() + 1);

    String intervalNum;
    String numOfRoads;

    public SystemRunnable() {
        systemStartTimeInMillis = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (isActive) {
            incrementTime();
            try {
                Thread.sleep(1000);
                decrementInterval();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    synchronized void startRoadManSystem(String intervalNum) {
        intervalDigit = new AtomicInteger(Integer.parseInt(intervalNum));
    }

    synchronized void decrementInterval() {
        if (intervalNum != null && intervalDigit.intValue() > 1) {
            intervalDigit.getAndDecrement();
        }
        if (intervalNum != null && intervalDigit.intValue() <= 1) {
            intervalDigit.set(Integer.parseInt(intervalNum));
        }
    }

    synchronized void closeCurrentRoad(int start) {

        if (roadQueue != null) {
            //change amount of current open road to max time

            int filledSlotsCount = roadQueue.length() - availableSlotCount.intValue();
            int newVal = Integer.parseInt(intervalNum) * (filledSlotsCount - 1) == 0 ? Integer.parseInt(intervalNum) :
                    Integer.parseInt(intervalNum) * (filledSlotsCount - 1);

            intervalDigit.set(newVal);

            roadQueue.getAndUpdate(start, r -> new String[]{roadQueue.get(start)[0], String.valueOf(newVal), "closed"});
        }
    }

    synchronized void openNewRoad(int start) {
        //change value to interval for countdown
        roadQueue.getAndUpdate(start, r -> new String[]{roadQueue.get(start)[0], intervalNum, "open"});
    }

    synchronized void incrementTime() {
        long currentTimeInMillis = System.currentTimeMillis();
        timeRunningInSecs = (currentTimeInMillis - systemStartTimeInMillis) / 1000;

        if (isInSystemState) {
            printSystemState();
        }
    }

    synchronized long getRunningTimeInSec() {
        return timeRunningInSecs;
    }

    synchronized void addRoadToQueue(String roadName) {

        if (availableSlotCount.intValue() == 0) {
            System.out.println("Queue is full");
            return;
        }

        roadQueue.set(enqueuePointer.get(), new String[]{roadName, null, null});
        int start = enqueuePointer.intValue();

        addtoTrafficSys(roadName, start);

        System.out.println(roadName + " Added!");

        enqueuePointer.getAndAdd(1);
        enqueuePointer.set(enqueuePointer.get() % roadQueue.length());

        availableSlotCount.getAndDecrement();
    }

    synchronized void addtoTrafficSys(String roadName, int start) {

        int currentInterval = 0;
        String state;

        if (start == openPointer.intValue()) {
            currentInterval = Integer.parseInt(intervalNum);
        } else if (start == nextToOpenPointer.intValue()) {
            currentInterval = Integer.parseInt(roadQueue.get(openPointer.intValue())[1]);
        } else {
            int prev1 = enqueuePointer.intValue() == 0 ? roadQueue.length() - 1 : enqueuePointer.intValue() - 1;
            if (!roadQueue.get(prev1)[1].isBlank()){
                currentInterval = Integer.parseInt(roadQueue.get(prev1)[1]) + Integer.parseInt(intervalNum);
            }
        }

        if (start == 0 && roadQueue.get(roadQueue.length() - 1) == null) {
            state = "open";
        } else {
            state = "closed";
        }

        int finalInterval = currentInterval;
        roadQueue.getAndUpdate(start, r -> new String[]{roadName, String.valueOf(finalInterval), state});
    }

    synchronized void deleteRoadFromQueue() {
        if (availableSlotCount.intValue() == roadQueue.length()) {
            System.out.println("Queue is empty");
            return;
        }

        int intPointer = dequeuePointer.intValue();

        if (intPointer + 1 < roadQueue.length() && roadQueue.get(intPointer + 1) != null) {
            deleteFromTrafficSys(intPointer + 1, intPointer);
        }

        System.out.println(roadQueue.get(intPointer)[0] + " deleted!");

        roadQueue.set(intPointer, new String[]{"", "", ""});

        dequeuePointer.getAndAdd(1);
        dequeuePointer.set(dequeuePointer.get() % roadQueue.length());

        availableSlotCount.getAndIncrement();
    }

    synchronized void deleteFromTrafficSys(int start, int deletedRoad) {

        if (roadQueue.get(deletedRoad)[2].equals("open") ) {
            intervalNum = roadQueue.get(start)[1];
        }
        if (roadQueue.get(deletedRoad)[2].equals("closed") ) {
            int i = start;
            int currentInterval = 0;

            while (!roadQueue.get(openPointer.intValue())[1].isBlank() && roadQueue.get(i) != null && !roadQueue.get(i)[0].isEmpty()) {
                if (Integer.parseInt(roadQueue.get(i)[1]) != Integer.parseInt(roadQueue.get(openPointer.intValue())[1])) {
                    currentInterval = Integer.parseInt(roadQueue.get(i)[1]) + Integer.parseInt(intervalNum);
                    roadQueue.set(i, new String[]{roadQueue.get(i)[0], String.valueOf(currentInterval), roadQueue.get(i)[2]});
                }

                i = (i + 1) % roadQueue.length();
                if (i == deletedRoad) { //prints number at dequeue
                    break;
                }
            }
        }
    }

    synchronized void printSystemState() {
        //   Main.clearConsole();

        System.out.println("! " + getRunningTimeInSec() + "s. have passed since system startup" + " !");
        System.out.println("! " + "Number of roads: " + numOfRoads + " !");
        System.out.println("! " + "Interval: " + intervalNum + " !");

        //dequeue is beginning of line and enqueue is end of line

        if (availableSlotCount.intValue() != roadQueue.length()) { //if has roads
            int start = dequeuePointer.intValue();
            System.out.println();
            while (!roadQueue.get(start)[0].isEmpty() && roadQueue.get(start) != null) {
                String countdown = getRoadCountDown(start);
                String state = roadQueue.get(start)[2];
                String ending = state.equals("open") ? "\u001B[32m" + state + " for " + countdown + "s" + "\u001B[0m" :
                        "\u001B[31m" + state + " for " + countdown + "s" + "\u001B[0m";
                System.out.println(roadQueue.get(start)[0] + " will be " + ending);
                int next = (start + 1) % roadQueue.length();
                if (next == enqueuePointer.intValue()) { //prints number at dequeue
                    break;
                }
                start = next;
            }
        }
        System.out.println();
        System.out.println("! " + "Press \"Enter\" to open menu" + " !");
    }

    volatile boolean hasOpenRoad;

    synchronized String getRoadCountDown(int start) {
        String countdown = roadQueue.get(start)[1];

        if (countdown != null && !countdown.isBlank() && roadQueue.length() > 1) {
            decrementCountdown(start);
        } else if (roadQueue.length() > 1) {
            addtoTrafficSys(roadQueue.get(start)[0], enqueuePointer.intValue());
            hasOpenRoad = true;
        }
        return roadQueue.get(start)[1];
    }

    synchronized void decrementCountdown(int start) {
        String roadName = roadQueue.get(start)[0];
        int countdown = Integer.parseInt(roadQueue.get(start)[1]);
        String state = roadQueue.get(start)[2];
        int usedSlots = roadQueue.length() - availableSlotCount.intValue();

        if (roadQueue.get(start)[1].isBlank()) { //IF UPDATED WITH A NEW ROAD
            return;
        }

        //if only 1 road is cycling through
        if (usedSlots == 1 && countdown == 1) {
            openNewRoad(start);
            return;
        }

        //updating by decrementing
        if (state.equals("open") && countdown == 1) {
            closeCurrentRoad(start);
        } else if (state.equals("closed") && countdown == 1) {
            openNewRoad(start);
            openPointer = new AtomicInteger(start);
        } else if (!roadQueue.get(start)[1].isBlank()) {
            int decrement;
            if (start == 0) { //if after deleting everything, you start adding again, you need to make sure index 0 gets max interval
                decrement = countdown - 1 == 0 ? Integer.parseInt(intervalNum) : countdown - 1;
            } else {
                decrement = countdown - 1 == 0 ? Integer.parseInt(intervalNum) * start : countdown - 1;
            }
            roadQueue.getAndUpdate(start, r -> new String[]{roadName, String.valueOf(decrement), state});
        }
    }

    synchronized void setSystemState(boolean isInSystemState, String numOfRoads) {
        this.isInSystemState = isInSystemState;
        this.numOfRoads = numOfRoads;
    }

    synchronized void setRoadState(AtomicReferenceArray<String[]> roadQueue, AtomicInteger enqueuePointer,
                                   AtomicInteger dequeuePointer, AtomicInteger availableSlotCount, String intervalNum) {
        this.roadQueue = roadQueue;
        this.enqueuePointer = enqueuePointer;
        this.dequeuePointer = dequeuePointer;
        this.availableSlotCount = availableSlotCount;
        this.intervalNum = intervalNum;
    }

    synchronized void exit() {
        isActive = false;
    }
}

public class Main {
    Scanner in = new Scanner(System.in);
    String[] menu = {
            "Quit", "Add road", "Delete road", "Open system"
    };

    AtomicReferenceArray<String[]> roadQueue;
    AtomicInteger enqueuePointer = new AtomicInteger(0);
    AtomicInteger dequeuePointer = new AtomicInteger(0);
    AtomicInteger availableSlotCount = new AtomicInteger(0);

    SystemRunnable sysRun = new SystemRunnable();

    public static void main(String[] args) {
        System.out.println("Welcome to the traffic management system!");
        new Main().getUserRoadConfig();
    }

    private void getUserRoadConfig() {
        System.out.print("Input the number of roads: ");
        String numOfRoads = in.nextLine();

        while (getInputNum(numOfRoads) <= 0) {
            System.out.print("Error! Incorrect input. Try again: ");
            numOfRoads = in.nextLine();
        }

        setRoadQueue(numOfRoads);

        System.out.print("Input the interval: ");
        String intervalNum = in.nextLine();
        while (getInputNum(intervalNum) <= 0) {
            System.out.print("Error! Incorrect input. Try again: ");
            intervalNum = in.nextLine();
        }

        printMenu();


        Thread queueThread = new Thread(sysRun);
        queueThread.setName("QueueThread");
        queueThread.start();

        boolean isRoadManSysRunning = false;

        while (true) {

            String menuOption = in.nextLine();
            int menuNum = getInputNum(menuOption);

            if (menuNum == 0) {
                System.out.println("Bye!");
                closeResources(in);
                sysRun.exit();
                try {
                    queueThread.join(); // Wait for it to actually stop
                } catch (InterruptedException ignored) {
                }
                return;
            }

            if (menuNum > 0 && menuNum < menu.length) {
                sysRun.setRoadState(roadQueue, enqueuePointer, dequeuePointer, availableSlotCount, intervalNum);
                switch (menuNum) {
                    case 1 -> {
                        System.out.print("Input road name: ");
                        String roadName = in.nextLine();

                        sysRun.addRoadToQueue(roadName);

                        if (!isRoadManSysRunning) { //as soon as we add our first road we start interval countdown
                            sysRun.startRoadManSystem(intervalNum);
                            isRoadManSysRunning = true;
                        }
                    }
                    case 2 -> sysRun.deleteRoadFromQueue();
                    case 3 -> sysRun.setSystemState(true, numOfRoads);
                }
                in.nextLine();
            } else {
                System.out.println("Incorrect option");
                in.nextLine();
            }

            sysRun.setSystemState(false, numOfRoads);

            printMenu();

            // clearConsole();
        }
    }

    void setRoadQueue(String numOfRoads) {
        int roadCount = Integer.parseInt(numOfRoads);
        roadQueue = new AtomicReferenceArray<>(roadCount);
        availableSlotCount.set(roadCount);
    }

    static void clearConsole() {
        try {
            var clearCommand = System.getProperty("os.name").contains("Windows")
                    ? new ProcessBuilder("cmd", "/c", "cls")
                    : new ProcessBuilder("clear");
            clearCommand.inheritIO().start().waitFor();
        } catch (IOException | InterruptedException ignored) {

        }
    }

    int getInputNum(String input) {
        int num = -1;
        try {
            num = Integer.parseInt(input);
        } catch (NumberFormatException nfe) {

        }
        return num;
    }

    void printMenu() {
        System.out.println("Menu:");
        for (int i = 1; i < menu.length; i++) {
            System.out.println(i + ". " + menu[i]);
        }
        System.out.println("0. " + menu[0]);
    }

    void closeResources(Scanner in) {
        in.close();
    }
}