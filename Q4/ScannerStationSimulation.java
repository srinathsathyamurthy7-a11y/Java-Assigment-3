package Q4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ScannerStationSimulation {

    // Way 1: Extending Thread
    static class StationThread extends Thread {
        private final int batchSize;
        private final AtomicInteger totalScanned;
        private final List<String> completionOrder;

        public StationThread(String name, int batchSize, AtomicInteger totalScanned, List<String> completionOrder) {
            super(name);
            this.batchSize = batchSize;
            this.totalScanned = totalScanned;
            this.completionOrder = completionOrder;
        }

        @Override
        public void run() {
            try {
                for (int i = 1; i <= batchSize; i++) {
                    Thread.sleep(20); // TIMED_WAITING state
                    totalScanned.incrementAndGet();
                }
                completionOrder.add(getName());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Way 2: Implementing Runnable (Preferred for decoupling task from thread)
    static class StationRunnable implements Runnable {
        private final String stationName;
        private final int batchSize;
        private final AtomicInteger totalScanned;
        private final List<String> completionOrder;

        public StationRunnable(String stationName, int batchSize, AtomicInteger totalScanned,
                List<String> completionOrder) {
            this.stationName = stationName;
            this.batchSize = batchSize;
            this.totalScanned = totalScanned;
            this.completionOrder = completionOrder;
        }

        @Override
        public void run() {
            try {
                for (int i = 1; i <= batchSize; i++) {
                    Thread.sleep(15);
                    totalScanned.incrementAndGet();
                }
                completionOrder.add(stationName);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Experiment A: Lifecycle states + join() + isAlive() using Thread subclass
    public static void runThreadSubclassDemo() throws InterruptedException {
        System.out.println("=== 1. EXTENDING THREAD: LIFE CYCLE, isAlive() & join() ===");
        AtomicInteger total = new AtomicInteger(0);
        List<String> order = Collections.synchronizedList(new ArrayList<String>());

        StationThread s1 = new StationThread("Station-1", 5, total, order);
        StationThread s2 = new StationThread("Station-2", 5, total, order);
        StationThread s3 = new StationThread("Station-3", 5, total, order);
        StationThread s4 = new StationThread("Station-4", 5, total, order);

        System.out.println("Before start() -> State: " + s1.getState() + " | isAlive: " + s1.isAlive());

        s1.start();
        s2.start();
        s3.start();
        s4.start();
        System.out.println("After start()  -> State: " + s1.getState() + " | isAlive: " + s1.isAlive());

        // Wait for all 4 stations to finish before printing final total
        s1.join();
        s2.join();
        s3.join();
        s4.join();

        System.out.println("After join()   -> State: " + s1.getState() + " | isAlive: " + s1.isAlive());
        System.out.println("Completion Order: " + order + " | Total Scripts Scanned: " + total.get() + "\n");
    }

    // Experiment B: Runnable + 10 Runs + Priorities
    public static void run10RunsWithRunnable() throws InterruptedException {
        System.out.println("=== 2. IMPLEMENTING RUNNABLE: 10 RUNS & PRIORITY EXPERIMENT ===");
        for (int run = 1; run <= 10; run++) {
            AtomicInteger total = new AtomicInteger(0);
            List<String> order = Collections.synchronizedList(new ArrayList<String>());

            Thread t1 = new Thread(new StationRunnable("S1(MIN)", 4, total, order));
            Thread t2 = new Thread(new StationRunnable("S2(NORM)", 4, total, order));
            Thread t3 = new Thread(new StationRunnable("S3(NORM)", 4, total, order));
            Thread t4 = new Thread(new StationRunnable("S4(MAX)", 4, total, order));

            t1.setPriority(Thread.MIN_PRIORITY); // 1
            t2.setPriority(Thread.NORM_PRIORITY); // 5
            t3.setPriority(Thread.NORM_PRIORITY); // 5
            t4.setPriority(Thread.MAX_PRIORITY); // 10

            t1.start();
            t2.start();
            t3.start();
            t4.start();
            t1.join();
            t2.join();
            t3.join();
            t4.join();

            System.out.printf("Run %2d -> Finish Order: %-38s | Total: %d%n", run, order, total.get());
        }
        System.out.println();
    }

    // Experiment C: Calling run() directly vs start()
    public static void proveRunVsStart() {
        System.out.println("=== 3. CALLING run() DIRECTLY vs start() ===");
        Runnable checkThread = () -> System.out.println("Executed inside thread: " + Thread.currentThread().getName());
        Thread t = new Thread(checkThread, "Scanner-Worker");

        System.out.print("Calling t.run() directly -> ");
        t.run(); // Runs synchronously on 'main' thread

        System.out.print("Calling t.start()        -> ");
        t.start(); // Spawns a new OS/JVM thread 'Scanner-Worker'
        try {
            t.join();
        } catch (InterruptedException ignored) {
        }
        System.out.println();
    }

    // Experiment D: Daemon thread behavior when main() ends
    public static void demoDaemonThread() throws InterruptedException {
        System.out.println("=== 4. DAEMON THREAD EXPERIMENT ===");
        Thread daemonStation = new Thread(() -> {
            try {
                for (int i = 1; i <= 10; i++) {
                    System.out.println("[Daemon Station] Scanning batch " + i + "...");
                    Thread.sleep(40);
                }
                System.out.println("[Daemon Station] Finished all 10 batches! (Never reached if main exits early)");
            } catch (InterruptedException ignored) {
            }
        }, "Daemon-Station");

        daemonStation.setDaemon(true);
        daemonStation.start();
        Thread.sleep(95); // Main only waits for ~2-3 batches, then exits without join()
        System.out.println("[Main Thread] Ending now -> JVM will immediately terminate Daemon-Station!");
    }

    public static void main(String[] args) throws InterruptedException {
        runThreadSubclassDemo();
        run10RunsWithRunnable();
        proveRunVsStart();
        demoDaemonThread();
    }
}