package Q3;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ManualReportGenerator {

    // Tracks unclosed file handles to verify leaks experimentally
    static int openFileHandles = 0;

    static class TrackedReader extends BufferedReader {
        private final String fileName;
        private boolean closed = false;

        public TrackedReader(String fileName) throws IOException {
            super(new FileReader(fileName));
            this.fileName = fileName;
            openFileHandles++;
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                openFileHandles--;
                super.close();
            }
        }
    }

    static class TrackedWriter extends BufferedWriter {
        private boolean closed = false;

        public TrackedWriter(String fileName) throws IOException {
            super(new FileWriter(fileName));
            openFileHandles++;
        }

        @Override
        public void close() throws IOException {
            if (!closed) {
                closed = true;
                openFileHandles--;
                super.close();
            }
        }
    }

    public static void generateManual(List<String> storeFiles, String summaryFile) {
        TrackedWriter writer = null;
        List<TrackedReader> openedReaders = new ArrayList<>();
        String currentFile = "NONE";

        try {
            writer = new TrackedWriter(summaryFile);
            double grandTotal = 0.0;

            for (String file : storeFiles) {
                currentFile = file;
                TrackedReader reader = new TrackedReader(file);
                openedReaders.add(reader);

                String line = reader.readLine();
                if (line == null || line.trim().isEmpty()) {
                    throw new IOException("Corrupt/empty store file detected: " + file);
                }
                double storeSales = Double.parseDouble(line.trim()); // throws NumberFormatException on corrupt data
                grandTotal += storeSales;
                writer.write(file + ": Rs. " + storeSales + "\n");
            }
            writer.write("GRAND TOTAL: Rs. " + grandTotal + "\n");

        } catch (IOException | NumberFormatException e) {
            System.out.println("[MANUAL ERROR LOG] Failed while processing file: '" + currentFile + "' -> " + e);
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                for (int i = 0; i < openedReaders.size(); i++) {
                    if (i == 1) {
                        // Simulate OS/stream close failure on the 2nd handle, aborting remaining closes
                        throw new IOException("Simulated close() failure on handle #" + i);
                    }
                    openedReaders.get(i).close();
                }
            } catch (IOException closeEx) {
                System.out.println("[MANUAL FINALLY ERROR] Cleanup aborted early: " + closeEx.getMessage());
            }
        }
    }

    public static void setupSampleFiles() throws IOException {
        new File("Q3").mkdirs();
        try (FileWriter w1 = new FileWriter("Q3/store1.txt")) {
            w1.write("12500.50\n");
        }
        try (FileWriter w2 = new FileWriter("Q3/store2.txt")) {
            w2.write("18400.00\n");
        }
        try (FileWriter w3 = new FileWriter("Q3/store3_corrupt.txt")) {
            w3.write("INVALID_SALES_DATA\n");
        }
        try (FileWriter w4 = new FileWriter("Q3/store4.txt")) {
            w4.write("9200.00\n");
        }
    }

    public static void main(String[] args) throws IOException {
        setupSampleFiles();

        List<String> validFiles = new ArrayList<>();
        validFiles.add("Q3/store1.txt");
        validFiles.add("Q3/store2.txt");

        openFileHandles = 0;
        generateManual(validFiles, "Q3/manual_valid_summary.txt");
        System.out.println("Manual Version (Valid Run)   -> Leaked open handles: " + openFileHandles);

        List<String> failingFiles = new ArrayList<>();
        failingFiles.add("Q3/store1.txt");
        failingFiles.add("Q3/store2.txt");
        failingFiles.add("Q3/store3_corrupt.txt");
        failingFiles.add("Q3/store4.txt");

        openFileHandles = 0;
        generateManual(failingFiles, "Q3/manual_failed_summary.txt");
        System.out.println("Manual Version (Failing Run) -> Leaked open handles: " + openFileHandles);
    }
}