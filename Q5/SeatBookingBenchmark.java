package Q5;

public class SeatBookingBenchmark {

    // 1. Unsynchronized version (contains check-then-act race condition)
    static class UnsynchronizedBooking {
        private final boolean[] booked;
        private int bookingsMade = 0;

        UnsynchronizedBooking(int seats) {
            booked = new boolean[seats];
        }

        public boolean book(int seat) {
            if (!booked[seat]) {
                // Tiny yield widens the interleaving window to reliably expose the race
                Thread.yield();
                booked[seat] = true;
                bookingsMade++;
                return true;
            }
            return false;
        }
    }

    // 2. Synchronized Method version (locks entire method on 'this')
    static class SyncMethodBooking {
        private final boolean[] booked;
        private int bookingsMade = 0;

        SyncMethodBooking(int seats) {
            booked = new boolean[seats];
        }

        public synchronized boolean book(int seat) {
            if (!booked[seat]) {
                booked[seat] = true;
                bookingsMade++;
                return true;
            }
            return false;
        }
    }

    // 3. Synchronized Block version (locks only the critical section)
    static class SyncBlockBooking {
        private final boolean[] booked;
        private int bookingsMade = 0;
        private final Object lock = new Object();

        SyncBlockBooking(int seats) {
            booked = new boolean[seats];
        }

        public boolean book(int seat) {
            // Non-critical validation outside lock
            if (seat < 0 || seat >= booked.length)
                return false;

            // Critical Section only
            synchronized (lock) {
                if (!booked[seat]) {
                    booked[seat] = true;
                    bookingsMade++;
                    return true;
                }
                return false;
            }
        }
    }

    // Student A: Reproduce race condition across 10 runs (100 threads competing for
    // 10 seats)
    private static void reproduceRaceOver10Runs() throws InterruptedException {
        System.out.println("=== 1. UNSYNCHRONIZED RACE REPRODUCTION (10 RUNS, 10 SEATS, 100 USERS) ===");
        int totalDoubleBookings = 0;
        for (int run = 1; run <= 10; run++) {
            UnsynchronizedBooking screen = new UnsynchronizedBooking(10);
            Thread[] users = new Thread[100];
            for (int i = 0; i < users.length; i++) {
                final int seat = i % 10;
                users[i] = new Thread(() -> screen.book(seat));
                users[i].start();
            }
            for (Thread t : users)
                t.join();
            int doubleSold = Math.max(0, screen.bookingsMade - 10);
            totalDoubleBookings += doubleSold;
            System.out.printf("Run %2d -> Seats sold: %2d (Expected: 10 | Double bookings: %d)%n",
                    run, screen.bookingsMade, doubleSold);
        }
        System.out.println("Total extra double-booked tickets across 10 runs: " + totalDoubleBookings + "\n");
    }

    // Student B: Measure time for 10,000 bookings across all 3 versions
    private static void benchmark10000Bookings() throws InterruptedException {
        System.out.println("=== 2. PERFORMANCE COMPARISON: 10,000 CONCURRENT BOOKING ATTEMPTS ===");
        final int totalAttempts = 10_000;
        final int numSeats = 1_000;
        final int numThreads = 10;
        final int attemptsPerThread = totalAttempts / numThreads;

        // A. Unsynchronized
        UnsynchronizedBooking unsync = new UnsynchronizedBooking(numSeats);
        long start = System.nanoTime();
        Thread[] t1 = new Thread[numThreads];
        for (int t = 0; t < numThreads; t++) {
            final int offset = t * attemptsPerThread;
            t1[t] = new Thread(() -> {
                for (int i = 0; i < attemptsPerThread; i++) {
                    unsync.book((offset + i) % numSeats);
                }
            });
            t1[t].start();
        }
        for (Thread t : t1)
            t.join();
        double unsyncMs = (System.nanoTime() - start) / 1_000_000.0;

        // B. Synchronized Method
        SyncMethodBooking syncMethod = new SyncMethodBooking(numSeats);
        start = System.nanoTime();
        Thread[] t2 = new Thread[numThreads];
        for (int t = 0; t < numThreads; t++) {
            final int offset = t * attemptsPerThread;
            t2[t] = new Thread(() -> {
                for (int i = 0; i < attemptsPerThread; i++) {
                    syncMethod.book((offset + i) % numSeats);
                }
            });
            t2[t].start();
        }
        for (Thread t : t2)
            t.join();
        double methodMs = (System.nanoTime() - start) / 1_000_000.0;

        // C. Synchronized Block
        SyncBlockBooking syncBlock = new SyncBlockBooking(numSeats);
        start = System.nanoTime();
        Thread[] t3 = new Thread[numThreads];
        for (int t = 0; t < numThreads; t++) {
            final int offset = t * attemptsPerThread;
            t3[t] = new Thread(() -> {
                for (int i = 0; i < attemptsPerThread; i++) {
                    syncBlock.book((offset + i) % numSeats);
                }
            });
            t3[t].start();
        }
        for (Thread t : t3)
            t.join();
        double blockMs = (System.nanoTime() - start) / 1_000_000.0;

        System.out.printf("Unsynchronized      -> Time: %7.3f ms | Seats Sold: %4d (Corrupted!)%n", unsyncMs,
                unsync.bookingsMade);
        System.out.printf("Synchronized Method -> Time: %7.3f ms | Seats Sold: %4d (Exact: 1000)%n", methodMs,
                syncMethod.bookingsMade);
        System.out.printf("Synchronized Block  -> Time: %7.3f ms | Seats Sold: %4d (Exact: 1000)%n", blockMs,
                syncBlock.bookingsMade);
    }

    public static void main(String[] args) throws InterruptedException {
        reproduceRaceOver10Runs();
        benchmark10000Bookings();
    }
}