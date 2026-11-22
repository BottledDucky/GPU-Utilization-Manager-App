package tests;
import model.*;
import storage.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public class StorageTest {
    private static void check(
            boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
    public static void main(String[] args) throws Exception {

        Path path = Files.createTempFile(
            "gpu-storage-test-", ".txt"
        );

        Files.delete(path);
        try {
            DataStore store = new DataStore(path.toFile());

            //checks missing file
            check(
                !store.load().isPresent(),
                "Missing file should load as empty"
            );

            //creating test data
            LocalDateTime start =
                LocalDateTime.of(2026, 11, 16, 10, 0);
            LocalDateTime now = start.plusMinutes(7);
            TimeAllowance allowance = new TimeAllowance(100);
            allowance.reserve("J101", 15);
            allowance.grantTime(
                20,
                "admin",
                "Extra test time",
                now
            );

            Student student = new Student("100067158","Abdulsalam ElHallak","100067158@ku.ac.ae","passwordHash","0501234567",allowance);

            Job job = new Job("J101",student.getUniId(),"OOP training","Java","Line one\nLine two\twith tab","MEDIUM",start);
            job.start(1, start);
            //Save data to the file
            SavedState state = new SavedState();
            state.setClock(start, now);
            state.setNextJobNumber(102);
            state.addStudent(student);
            state.addJob(job);
            state.assignGpu(1, "J101");
            store.save(state);
            String text = new String(
                Files.readAllBytes(path),
                StandardCharsets.UTF_8
            );

            check(text.startsWith("GPU_STATE_TEXT_V1"),"Missing text header");
            check(text.contains("STUDENT\t100067158"),"Student is not readable text");
            check(text.contains("JOB\tJ101"),"Job is not readable text");
            check(text.contains("Extra test time"),"Grant reason is not readable text");
            //loading saved data
            SavedState restored = null;

            if (store.load().isPresent()) {restored = store.load().get();}
            else {throw new AssertionError("No state loaded");}
            check(now.equals(restored.getClockNow()),"Clock not restored");
            check(restored.getNextJobNumber() == 102,"Next job number not restored");
            check("J101".equals(restored.getGpuAssignments().get(1)), "GPU not restored");
            Student loadedStudent = restored.restoreStudents().get("100067158");
            check(loadedStudent != null, "Student not restored");
            check(loadedStudent.getTimeAllowance().getReserved() == 15, "Reservation not restored");
            check(loadedStudent.getTimeAllowance().getAllocated() == 120, "Grant not restored");
            check(restored.restoreJobs().get(0).getDescription().equals(job.getDescription()), "Multiline description not restored");

            //corrupted file
            Files.write(path, "BROKEN DATA".getBytes(StandardCharsets.UTF_8));

            boolean rejected = false;

            try {
                store.load();
            } catch (IOException expected) {
                rejected = true;
            }

            check(rejected, "Corrupt file should be rejected");

            System.out.println("StorageTest PASSED");

        } finally {
            Files.deleteIfExists(path);
        }
    }
}
