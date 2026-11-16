package Q2;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

// Checked domain exception hierarchy (caller must handle or declare throws)
class PortalExamException extends Exception {
    public PortalExamException(String message) {
        super(message);
    }

    public PortalExamException(String message, Throwable cause) {
        super(message, cause);
    }
}

class ExamWindowClosedException extends PortalExamException {
    public ExamWindowClosedException(String message) {
        super(message);
    }
}

class PortalStorageException extends PortalExamException {
    public PortalStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Unchecked exception for programmer/session state violation
class StudentNotRegisteredException extends RuntimeException {
    public StudentNotRegisteredException(String message) {
        super(message);
    }
}

// Layer 3: Storage Layer
class SubmissionStorageLayer {
    public void persistFile(String studentId, String fileName, long sizeKb) throws PortalStorageException {
        try {
            if (sizeKb > 500) {
                throw new IOException("Disk quota exceeded (500 KB max, received " + sizeKb + " KB)");
            }
            System.out.println("[Storage] Saved " + fileName + " for " + studentId);
        } catch (IOException ioEx) {
            // Chaining preserves the low-level IOException inside a domain-meaningful
            // exception
            throw new PortalStorageException("Storage failure while saving '" + fileName + "'", ioEx);
        }
    }
}

// Layer 2: Service Layer
class ExamSubmissionService {
    private final Set<String> registeredStudents = new HashSet<>(Arrays.asList("192511064", "192511001"));
    private final SubmissionStorageLayer storage = new SubmissionStorageLayer();

    public void validateAndSubmit(String studentId, boolean windowOpen, String fileName, long sizeKb)
            throws ExamWindowClosedException, PortalStorageException {
        if (!windowOpen) {
            throw new ExamWindowClosedException("Submission rejected: Exam window closed at 17:00 IST.");
        }
        if (!registeredStudents.contains(studentId)) {
            throw new StudentNotRegisteredException("Access denied: Student " + studentId + " is not enrolled.");
        }
        // Propagates PortalStorageException upward to Controller
        storage.persistFile(studentId, fileName, sizeKb);
    }
}

// Layer 1: Controller Layer (UI Boundary)
class ExamSubmissionController {
    private final ExamSubmissionService service = new ExamSubmissionService();

    public void handleUploadRequest(String studentId, boolean windowOpen, String fileName, long sizeKb) {
        System.out
                .println("\n--- Submitting: student=" + studentId + ", file=" + fileName + " (" + sizeKb + " KB) ---");
        try {
            service.validateAndSubmit(studentId, windowOpen, fileName, sizeKb);
            System.out.println("[Controller -> Student UI] SUCCESS: Answer sheet uploaded.");
        } catch (ExamWindowClosedException | PortalStorageException e) {
            System.out.println("[Controller -> Student UI] Submission Failed: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("[Internal Audit Log] Root Cause: " + e.getCause());
                e.printStackTrace(System.out);
            }
        } catch (StudentNotRegisteredException e) {
            System.out.println("[Controller -> Student UI] Registration Error: " + e.getMessage());
        }
    }
}

public class ExamPortalPropagation {
    public static void main(String[] args) {
        ExamSubmissionController controller = new ExamSubmissionController();

        // Scenario 1: Valid submission
        controller.handleUploadRequest("192511064", true, "co3_answers.pdf", 240);

        // Scenario 2: Exam window closed (Checked)
        controller.handleUploadRequest("192511064", false, "late_answers.pdf", 180);

        // Scenario 3: Student not registered (Unchecked)
        controller.handleUploadRequest("192599999", true, "guest_answers.pdf", 150);

        // Scenario 4: File exceeds size limit -> IOException chained into
        // PortalStorageException (Checked)
        controller.handleUploadRequest("192511064", true, "oversized_scan.pdf", 850);

        // Scenario 5: Demonstrating information lost if re-thrown WITHOUT cause
        System.out.println("\n--- Scenario 5: Re-throwing WITHOUT cause ---");
        try {
            try {
                throw new IOException("Disk quota exceeded");
            } catch (IOException e) {
                throw new PortalStorageException("Could not store file", null); // Cause omitted!
            }
        } catch (PortalStorageException e) {
            System.out.println("Error: " + e.getMessage());
            System.out.println("Cause object: " + e.getCause() + " (Original IOException stack trace lost!)");
        }
    }
}