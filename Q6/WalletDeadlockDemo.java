package Q6;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class WalletDeadlockDemo {

    static class WalletAccount {
        final int id;
        final String owner;
        double balance;

        WalletAccount(int id, String owner, double balance) {
            this.id = id;
            this.owner = owner;
            this.balance = balance;
        }
    }

    // 1. Deadlock-prone transfer (locks 'from' then 'to' in caller order ->
    // circular wait!)
    public static void transferDeadlockProne(WalletAccount from, WalletAccount to, double amount) {
        synchronized (from) {
            try {
                Thread.sleep(40);
            } catch (InterruptedException ignored) {
            }
            synchronized (to) {
                if (from.balance >= amount) {
                    from.balance -= amount;
                    to.balance += amount;
                }
            }
        }
    }

    // 2. Deadlock-free transfer (enforces strict global lock ordering by account
    // ID)
    public static void transferSafeOrdered(WalletAccount from, WalletAccount to, double amount) {
        WalletAccount firstLock = (from.id < to.id) ? from : to;
        WalletAccount secondLock = (from.id < to.id) ? to : from;

        synchronized (firstLock) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
            synchronized (secondLock) {
                if (from.balance >= amount) {
                    from.balance -= amount;
                    to.balance += amount;
                }
            }
        }
    }

    // Demonstrates reliable deadlock detection without hanging the JVM forever
    public static void demonstrateDeadlock() throws InterruptedException {
        System.out.println("=== 1. PART A (UNSAFE): RELIABLE DEADLOCK DEMONSTRATION ===");
        WalletAccount accA = new WalletAccount(101, "Arun", 1000.0);
        WalletAccount accB = new WalletAccount(102, "Bala", 1000.0);

        Thread t1 = new Thread(() -> transferDeadlockProne(accA, accB, 200.0), "Transfer-A-to-B");
        Thread t2 = new Thread(() -> transferDeadlockProne(accB, accA, 300.0), "Transfer-B-to-A");

        t1.setDaemon(true); // Daemon so JVM can exit cleanly after detection
        t2.setDaemon(true);
        t1.start();
        t2.start();

        Thread.sleep(150); // Give both threads time to lock their first account and block on the second

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] deadlockedIds = bean.findDeadlockedThreads();
        System.out.println("Deadlock detected by JVM ThreadMXBean? " + (deadlockedIds != null));
        System.out.println("Thread States -> " + t1.getName() + ": " + t1.getState()
                + " | " + t2.getName() + ": " + t2.getState() + "\n");
    }

    // Demonstrates deadlock-free transfers under heavy opposing concurrency
    public static void demonstrateDeadlockFree() throws InterruptedException {
        System.out.println("=== 2. PART A (SAFE): LOCK-ORDERING PREVENTION ===");
        WalletAccount accA = new WalletAccount(101, "Arun", 1000.0);
        WalletAccount accB = new WalletAccount(102, "Bala", 1000.0);

        Thread[] workers = new Thread[20];
        for (int i = 0; i < workers.length; i++) {
            final boolean forward = (i % 2 == 0);
            workers[i] = new Thread(() -> {
                for (int k = 0; k < 5; k++) {
                    if (forward)
                        transferSafeOrdered(accA, accB, 10.0);
                    else
                        transferSafeOrdered(accB, accA, 10.0);
                }
            });
            workers[i].start();
        }
        for (Thread w : workers)
            w.join();

        System.out.printf("All 100 opposing transfers completed! Arun: %.2f | Bala: %.2f | Total: %.2f%n%n",
                accA.balance, accB.balance, (accA.balance + accB.balance));
    }

    // Student A: Record counter size over time to prove 0 <= count <= 5
    public static void traceCounterBounds() throws InterruptedException {
        System.out.println("=== 3. PART B: COUNTER SIZE BOUNDS TRACE (0 <= count <= 5) ===");
        final int[] buffer = new int[5];
        final int[] state = { 0, 0, 0 }; // [count, in, out]
        final int[] minMax = { 0, 0 }; // [minObserved, maxObserved]
        final Object monitor = new Object();

        Thread cook = new Thread(() -> {
            try {
                for (int t = 1; t <= 12; t++) {
                    synchronized (monitor) {
                        while (state[0] == 5)
                            monitor.wait();
                        buffer[state[1]] = t;
                        state[1] = (state[1] + 1) % 5;
                        state[0]++;
                        minMax[1] = Math.max(minMax[1], state[0]);
                        System.out.println("[Cook put(" + t + ")]  -> Counter size = " + state[0]);
                        monitor.notifyAll();
                    }
                    Thread.sleep(5);
                }
            } catch (InterruptedException ignored) {
            }
        });

        Thread student = new Thread(() -> {
            try {
                for (int k = 1; k <= 12; k++) {
                    synchronized (monitor) {
                        while (state[0] == 0)
                            monitor.wait();
                        int val = buffer[state[2]];
                        state[2] = (state[2] + 1) % 5;
                        state[0]--;
                        minMax[0] = Math.min(minMax[0], state[0]);
                        System.out.println("[Student take(" + val + ")] -> Counter size = " + state[0]);
                        monitor.notifyAll();
                    }
                    Thread.sleep(12);
                }
            } catch (InterruptedException ignored) {
            }
        });

        cook.start();
        student.start();
        cook.join();
        student.join();
        System.out.println(
                "Observed Counter Size Range: Min = " + minMax[0] + ", Max = " + minMax[1] + " (Always within 0..5!)");
    }

    public static void main(String[] args) throws InterruptedException {
        demonstrateDeadlock();
        demonstrateDeadlockFree();
        traceCounterBounds();
    }
}