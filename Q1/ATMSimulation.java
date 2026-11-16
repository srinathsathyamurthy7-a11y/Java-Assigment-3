import java.io.File;
import java.io.FileNotFoundException;
import java.util.InputMismatchException;
import java.util.Scanner;

// Custom unchecked exception for business rule violations
class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}

public class ATMSimulation {

    // Simulates reading card/account config file (Checked Exception: FileNotFoundException)
    public static double loadAccountBalance(String cardFilePath, String accountNo) throws FileNotFoundException {
        File cardFile = new File(cardFilePath);
        if (!cardFile.exists()) {
            throw new FileNotFoundException("Card-reader file missing: " + cardFilePath);
        }
        try (Scanner fileScanner = new Scanner(cardFile)) {
            while (fileScanner.hasNext()) {
                String acc = fileScanner.next();
                double bal = Double.parseDouble(fileScanner.next());
                if (acc.equals(accountNo)) {
                    return bal;
                }
            }
        }
        throw new IllegalArgumentException("Account number not found in card registry: " + accountNo);
    }

    public static void processWithdrawal(String cardFilePath, String accountNo, String rawAmount) {
        boolean cardEjected = false;
        try {
            System.out.println("\n--- Starting ATM Session for Account: " + accountNo + " ---");

            // Failure Point 1: Card-reader file missing (Checked: FileNotFoundException)
            // Failure Point 2: Unknown account (Unchecked: IllegalArgumentException)
            double balance = loadAccountBalance(cardFilePath, accountNo);

            // Failure Point 3: Non-numeric input (Unchecked: NumberFormatException)
            double amount = Double.parseDouble(rawAmount.trim());

            // Failure Point 4: Negative or zero withdrawal (Unchecked: IllegalArgumentException)
            if (amount <= 0) {
                throw new IllegalArgumentException("Withdrawal amount must be positive. Entered: " + amount);
            }

            // Failure Point 5: Overdraft / insufficient balance (Unchecked: InsufficientFundsException)
            if (amount > balance) {
                throw new InsufficientFundsException(
                        String.format("Requested %.2f exceeds available balance %.2f", amount, balance));
            }

            balance -= amount;
            System.out.printf("SUCCESS: Dispensed Rs. %.2f | Remaining Balance: Rs. %.2f%n", amount, balance);

        } catch (FileNotFoundException e) {
            // Checked exception caught first
            System.out.println("[CAUGHT FileNotFoundException - Checked] " + e.getMessage());
        } catch (NumberFormatException | InputMismatchException e) {
            // Multi-catch for malformed numeric input (narrower than IllegalArgumentException!)
            System.out.println("[CAUGHT Multi-Catch NumberFormat/InputMismatch - Unchecked] Invalid number format: '" + rawAmount + "'");
        } catch (InsufficientFundsException e) {
            // Specific domain exception caught before general RuntimeException
            System.out.println("[CAUGHT InsufficientFundsException - Unchecked] " + e.getMessage());
        } catch (IllegalArgumentException e) {
            // Broader than NumberFormatException (since NumberFormatException extends IllegalArgumentException)
            System.out.println("[CAUGHT IllegalArgumentException - Unchecked] " + e.getMessage());
        } catch (Exception e) {
            // Fallback safety net
            System.out.println("[CAUGHT General Exception] " + e.getMessage());
        } finally {
            // Guaranteed cleanup regardless of success or any exception
            cardEjected = true;
            System.out.println("[FINALLY EXECUTED] Session closed. ATM Card Ejected = " + cardEjected);
        }
    }

    public static void main(String[] args) {
        // Test 1: Valid withdrawal (create temporary card_data.txt first)
        try {
            java.io.PrintWriter pw = new java.io.PrintWriter("Q1/card_data.txt");
            pw.println("ACC1001 5000.00");
            pw.close();
        } catch (Exception ignored) {}

        // 5+ Distinct Execution Paths for Student B Testing & Tracing
        processWithdrawal("Q1/card_data.txt", "ACC1001", "1500");       // Path 1: Normal Success
        processWithdrawal("Q1/missing_card.txt", "ACC1001", "500");    // Path 2: FileNotFoundException (Checked)
        processWithdrawal("Q1/card_data.txt", "ACC1001", "abc");        // Path 3: Multi-catch NumberFormatException
        processWithdrawal("Q1/card_data.txt", "ACC1001", "-200");       // Path 4: IllegalArgumentException
        processWithdrawal("Q1/card_data.txt", "ACC1001", "9000");       // Path 5: InsufficientFundsException
        processWithdrawal("Q1/card_data.txt", "ACC9999", "500");        // Path 6: Unknown Account (IllegalArgumentException)
    }
}