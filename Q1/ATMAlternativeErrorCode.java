import java.io.File;
import java.util.Scanner;

public class ATMAlternativeErrorCode {
    // Error codes instead of exceptions
    static final int ERR_FILE_MISSING = -1;
    static final int ERR_INVALID_NUMBER = -2;
    static final int ERR_NEGATIVE_AMOUNT = -3;
    static final int ERR_INSUFFICIENT_FUNDS = -4;

    public static int withdrawWithoutExceptions(String filePath, String rawAmount, double balance) {
        File f = new File(filePath);
        if (!f.exists())
            return ERR_FILE_MISSING;

        Scanner sc = new Scanner(rawAmount);
        if (!sc.hasNextDouble()) {
            sc.close();
            return ERR_INVALID_NUMBER;
        }
        double amount = sc.nextDouble();
        sc.close();

        if (amount <= 0)
            return ERR_NEGATIVE_AMOUNT;
        if (amount > balance)
            return ERR_INSUFFICIENT_FUNDS;

        return (int) (balance - amount);
    }

    public static void main(String[] args) {
        long startCode = System.nanoTime();
        int res = 0;
        for (int i = 0; i < 100_000; i++) {
            res += withdrawWithoutExceptions("Q1/card_data.txt", "-50", 5000.0);
        }
        double codeMs = (System.nanoTime() - startCode) / 1_000_000.0;

        long startEx = System.nanoTime();
        int caught = 0;
        for (int i = 0; i < 100_000; i++) {
            try {
                if (-50 <= 0)
                    throw new IllegalArgumentException("Negative");
            } catch (IllegalArgumentException e) {
                caught++;
            }
        }
        double exMs = (System.nanoTime() - startEx) / 1_000_000.0;

        System.out.printf(
                "100,000 Invalid Checks -> Error Code Return: %.3f ms | Exception Throw+Catch: %.3f ms (res=%d, caught=%d)%n",
                codeMs, exMs, res, caught);
    }
}