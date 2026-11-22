package ui;

import java.util.List;

import logic.SimulationClock;
import model.AccessRequest;
import model.GPU;
import model.Job;
import model.Student;

public interface AdminBackend {

    // --- Clock & Simulation (Member 2: Scheduler / SimulationClock) ---
    SimulationClock getClock();
    void advanceMinute();
    GPU[] getGpus();
    List<Job> getWaitingQueue();
    List<Job> getAllJobs();
    void cancelJob(String jobId, String cancelledBy, String reason);

    // --- Registration Requests & Accounts (Member 1: AccountService) ---
    List<AccessRequest> getAccessRequests();
    void approveRequest(AccessRequest request, int initialMinutes);
    void rejectRequest(AccessRequest request);

    // --- Students & Allowances (Member 1 & Member 4) ---
    List<Student> getAllStudents();
    Student findStudentById(String universityId);
    List<Job> getJobsForStudent(String universityId);
    void grantExtraTime(Student student, int minutes, String reason);

    // --- Navigation / Persistence ---
    void onLogout();
}
