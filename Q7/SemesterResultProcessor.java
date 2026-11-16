package Q7;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class SemesterResultProcessor {

    // Immutable result record for each processed student file
    static class FileOutcome {
        final String fileName;
        final boolean success;
        final String studentId;
        final double percentage;
        final String grade;
        final String errorReason;

        static FileOutcome ok(String fileName, String studentId, double percentage, String grade) {
            return new FileOutcome(fileName, true, studentId, percentage, grade, null);
        }

        static FileOutcome fail(String fileName, String errorReason) {
            return new FileOutcome(fileName, false, null, 0.0, null, errorReason);
        }

        private FileOutcome(String fileName, boolean success, String studentId,
                double percentage, String grade, String errorReason) {
            this.fileName = fileName;
            this.success = success;
            this.studentId = studentId;
            this.percentage = percentage;
            this.grade = grade;
            this.errorReason = errorReason;
        }
    }

    // Callable task: reads, validates, computes result, and isolates any file-level
    // failure
    static class ScoreFileTask implements Callable<FileOutcome> {
        private final File file;
        private final ConcurrentHashMap<String, String> validGrades;
        private final ConcurrentHashMap<String, String> failureLog;
        private final AtomicInteger successCount;
        private final AtomicInteger failureCount;

        ScoreFileTask(File file,
                ConcurrentHashMap<String, String> validGrades,
                ConcurrentHashMap<String, String> failureLog,
                AtomicInteger successCount,
                AtomicInteger failureCount) {
            this.file = file;
            this.validGrades = validGrades;
            this.failureLog = failureLog;
            this.successCount = successCount;
            this.failureCount = failureCount;
        }

        @Override
        public FileOutcome call() {
            String name = file.getName();
            if (!file.exists()) {
                failureCount.incrementAndGet();
                failureLog.put(name, "Missing file");
                return FileOutcome.fail(name, "Missing file");
            }

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line = br.readLine();
                if (line == null || line.trim().isEmpty()) {
                    throw new IllegalArgumentException("Empty file");
                }
                // Expected format: STUDENT_ID,M1,M2,M3,M4,M5
                String[] parts = line.trim().split(",");
                if (parts.length != 6) {
                    throw new IllegalArgumentException(
                            "Corrupt format (expected 6 fields, found " + parts.length + ")");
                }
                String studentId = parts[0].trim();
                double sum = 0.0;
                for (int i = 1; i <= 5; i++) {
                    double mark = Double.parseDouble(parts[i].trim());
                    if (mark < 0 || mark > 100) {
                        throw new IllegalArgumentException("Out-of-range mark: " + mark);
                    }
                    sum += mark;
                }
                double pct = sum / 5.0;
                String grade = (pct >= 90) ? "O" : (pct >= 80) ? "A+" : (pct >= 70) ? "A" : (pct >= 50) ? "B" : "RA";

                validGrades.put(studentId, String.format("%.2f%% (%s)", pct, grade));
                successCount.incrementAndGet();
                return FileOutcome.ok(name, studentId, pct, grade);

            } catch (Exception e) {
                // Fault isolation: record error without crashing worker or aborting other tasks
                failureCount.incrementAndGet();
                failureLog.put(name, e.getClass().getSimpleName() + ": " + e.getMessage());
                return FileOutcome.fail(name, e.getMessage());
            }
        }
    }

    static class BatchSummary {
        final int totalFiles;
        final int successes;
        final int failures;
        final double elapsedMs;
        final ConcurrentHashMap<String, String> validGrades;
        final ConcurrentHashMap<String, String> failureLog;

        BatchSummary(int totalFiles, int successes, int failures, double elapsedMs,
                ConcurrentHashMap<String, String> validGrades,
                ConcurrentHashMap<String, String> failureLog) {
            this.totalFiles = totalFiles;
            this.successes = successes;
            this.failures = failures;
            this.elapsedMs = elapsedMs;
            this.validGrades = validGrades;
            this.failureLog = failureLog;
        }
    }

    // Core Concurrent Processor using ExecutorService + Callable + Future
    public static BatchSummary processBatch(List<File> files, int poolSize)
            throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        ConcurrentHashMap<String, String> validGrades = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> failureLog = new ConcurrentHashMap<>();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        long start = System.nanoTime();
        List<Future<FileOutcome>> futures = new ArrayList<>(files.size());

        for (File f : files) {
            futures.add(executor.submit(new ScoreFileTask(f, validGrades, failureLog, successCount, failureCount)));
        }

        // Collect every Future to guarantee all 10,000 tasks complete before
        // summarizing
        for (Future<FileOutcome> future : futures) {
            future.get();
        }
        executor.shutdown();

        double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
        return new BatchSummary(files.size(), successCount.get(), failureCount.get(), elapsedMs, validGrades,
                failureLog);
    }

    // Generates test dataset (with injected corrupt and missing files)
    private static List<File> generateDataset(String dirPath, int totalCount) throws IOException {
        File dir = new File(dirPath);
        if (!dir.exists())
            dir.mkdirs();

        List<File> files = new ArrayList<>(totalCount);
        for (int i = 1; i <= totalCount; i++) {
            File f = new File(dir, String.format("script_%05d.csv", i));
            files.add(f);

            if (i % 25 == 0) {
                // Inject missing file (4% missing -> 400 out of 10,000; or 0 in 10-file check
                // if <25)
                if (f.exists())
                    f.delete();
            } else if (i % 20 == 0) {
                // Inject corrupt file (non-numeric mark)
                try (FileWriter fw = new FileWriter(f)) {
                    fw.write("REG" + i + ",85,90,CORRUPT,78,88\n");
                }
            } else if (i % 33 == 0) {
                // Inject out-of-range mark
                try (FileWriter fw = new FileWriter(f)) {
                    fw.write("REG" + i + ",85,145,70,78,88\n");
                }
            } else {
                // Valid score file
                try (FileWriter fw = new FileWriter(f)) {
                    int m1 = 60 + (i % 40);
                    int m2 = 65 + (i % 35);
                    int m3 = 70 + (i % 30);
                    int m4 = 55 + (i % 45);
                    int m5 = 75 + (i % 25);
                    fw.write("REG" + i + "," + m1 + "," + m2 + "," + m3 + "," + m4 + "," + m5 + "\n");
                }
            }
        }
        return files;
    }

    public static void main(String[] args) throws Exception {
        // 1. Hand-checked 10-file verification set (7 valid, 2 corrupt, 1 missing)
        System.out.println("=== 1. HAND-CHECKED VERIFICATION SET (10 FILES) ===");
        File checkDir = new File("Q7/verify_set");
        checkDir.mkdirs();
        List<File> handSet = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            File f = new File(checkDir, "valid_" + i + ".csv");
            try (FileWriter fw = new FileWriter(f)) {
                fw.write("STU0" + i + ",80,85,90,75,80\n");
            }
            handSet.add(f);
        }
        File corrupt1 = new File(checkDir, "corrupt_1.csv");
        try (FileWriter fw = new FileWriter(corrupt1)) {
            fw.write("STU08,80,ABC,90,75,80\n");
        }
        handSet.add(corrupt1);

        File corrupt2 = new File(checkDir, "corrupt_2.csv");
        try (FileWriter fw = new FileWriter(corrupt2)) {
            fw.write("STU09,80,150,90,75,80\n");
        }
        handSet.add(corrupt2);

        File missing1 = new File(checkDir, "missing_1.csv");
        if (missing1.exists())
            missing1.delete();
        handSet.add(missing1);

        BatchSummary verifySummary = processBatch(handSet, 4);
        System.out.printf("Total: %d | Expected Success: 7 -> Observed: %d | Expected Failures: 3 -> Observed: %d%n",
                verifySummary.totalFiles, verifySummary.successes, verifySummary.failures);
        System.out.println("Failure Log: " + verifySummary.failureLog + "\n");

        // 2. Full 10,000-file benchmark across thread pool sizes 1, 2, 4, and 8
        System.out.println("=== 2. SCALING BENCHMARK: 10,000 ANSWER-SCRIPT FILES ===");
        System.out.println("Generating 10,000 test files with injected missing & corrupt files...");
        List<File> tenThousandFiles = generateDataset("Q7/dataset_10k", 10_000);

        // Warmup run
        processBatch(tenThousandFiles.subList(0, 500), 4);

        int[] poolSizes = { 1, 2, 4, 8 };
        for (int pool : poolSizes) {
            BatchSummary res = processBatch(tenThousandFiles, pool);
            System.out.printf("Pool Size: %d threads | Time: %8.2f ms | Total: %d | Success: %d | Failed: %d%n",
                    pool, res.elapsedMs, res.totalFiles, res.successes, res.failures);
        }
    }
}