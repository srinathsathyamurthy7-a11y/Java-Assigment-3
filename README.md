# CSA0904 – Java Assignment 3 (CO3)
## Exception Handling, Resource Safety & Multithreading

### Student Details
- **Name:** PURUSHOTHAMAN.V
- **Register Number:** 192511064
- **Department:** Computer Science and Engineering

### Development Environment
- **IDE:** Visual Studio Code (VS Code)
- **Programming Language:** Java
- **JDK:** Java SE Development Kit 8 (JDK 1.8)
- **Operating System:** Windows
- **Terminal:** VS Code Integrated Terminal (PowerShell)

### Java Version
To check the installed Java version, execute:

```bash
java -version
javac -version
```

**Java Version:** Java SE Development Kit 8 (JDK 1.8)

### Compilation and Execution
All Java programs were developed, tested, and executed from the root repository folder:

```bash
# Q1 - Bank ATM Simulation & Error-Code Benchmark
javac Q1/ATMSimulation.java Q1/ATMAlternativeErrorCode.java
java -cp Q1 ATMSimulation
java -cp Q1 ATMAlternativeErrorCode

# Q2 - Online Exam Portal Exceptions, Propagation & Chaining
javac Q2/StorageDemo.java Q2/ExamPortalPropagation.java
java -cp Q2 StorageDemo
java -cp Q2 ExamPortalPropagation

# Q3 - Daily Sales Report Resource Safety & Try-With-Resources
javac Q3/ManualReportGenerator.java Q3/TryWithResourcesReportGenerator.java
java -cp . Q3.ManualReportGenerator
java -cp . Q3.TryWithResourcesReportGenerator

# Q4 - Parallel Answer-Script Scanning & Thread Life Cycle
javac Q4/ScannerStationSimulation.java
java -cp . Q4.ScannerStationSimulation

# Q5 - Cinema Seat-Booking Race Conditions & Synchronization
javac Q5/SeatBooking.java Q5/SeatBookingBenchmark.java
java -cp . Q5.SeatBooking
java -cp . Q5.SeatBookingBenchmark

# Q6 - Hostel Wallet Deadlock & Mess Token Producer-Consumer
javac Q6/TokenCounter.java Q6/WalletDeadlockDemo.java
java -cp . Q6.TokenCounter
java -cp . Q6.WalletDeadlockDemo

# Q7 - Concurrent Fault-Tolerant Semester Result Processor
javac Q7/SemesterResultProcessor.java
java -cp . Q7.SemesterResultProcessor
```

### Assignment Results

| Task | Description | Status | Output / Key Finding |
|---|---|---|---|
| Q1 | Bank ATM Exception Hierarchy (`try`, `multi-catch`, `finally`) | Completed | Handled checked & unchecked failures; `finally` guaranteed card ejection |
| Q2 | Online Exam Portal User-Defined Exceptions & Chaining | Completed | Preserved root `IOException` across 3 layers via `SubmissionStorageException` |
| Q3 | Resource Safety (`try-with-resources` vs Manual `close()`) | Completed | Reduced leaked handles from `2` to `0` on partial failure; captured suppressed exceptions |
| Q4 | Thread Creation (`Thread` vs `Runnable`) & Life Cycle | Completed | Verified `NEW -> RUNNABLE -> TERMINATED`, `join()` barrier, and daemon exit |
| Q5 | Cinema Seat-Booking Race Conditions & Synchronization | Completed | Eliminated double-bookings (`10/10` exact) using synchronized check-then-book |
| Q6 | Deadlock Prevention & Producer-Consumer (`wait`/`notifyAll`) | Completed | Prevented deadlock via ID lock ordering; maintained token counter bounds `0..5` |
| Q7 | Concurrent Fault-Tolerant Result Processor (`ExecutorService`) | Completed | Processed 10,000 files (`8920` valid, `1080` failed) safely with `ConcurrentHashMap` |

### Project Objectives
- Design and trace exception hierarchies, checked vs. unchecked exceptions, propagation, and chaining across layered applications.
- Prevent resource leaks and handle suppressed exceptions cleanly using `try-with-resources`.
- Analyse thread life cycles, race conditions, synchronization strategies, and deadlock prevention in multithreaded Java programs.
- Architect and benchmark a fault-tolerant concurrent file processor using `ExecutorService`, `Callable`, `Future`, and `ConcurrentHashMap`.

### Screenshots
Screenshots of the program executions and terminal outputs are maintained in their respective task folders (`Q1` through `Q7`).

### Conclusion
This assignment provided practical engineering experience in building fault-tolerant, resource-safe, and thread-safe Java applications using Visual Studio Code.

---
**Submitted by:** PURUSHOTHAMAN.V  
**Register Number:** 192511064
