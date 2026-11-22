package ui;

import java.util.ArrayList;
import java.util.List;

import logic.AccountService;
import logic.Scheduler;
import logic.SimulationClock;
import model.AccessRequest;
import model.Administrator;
import model.GPU;
import model.Job;
import model.Student;
import model.TimeAllowance;

// Connects Member 3's AdminDashboard to Member 1's AccountService,
// Member 2's Scheduler, and Member 4's storage save callback.
public class RealAdminBackend implements AdminBackend {

    private final Scheduler scheduler;
    private final AccountService accountService;
    private final Administrator adminUser;
    private final Runnable onSave;
    private final Runnable onLogoutCallback;

    public RealAdminBackend(
            Scheduler scheduler,
            AccountService accountService,
            Administrator adminUser,
            Runnable onSave,
            Runnable onLogoutCallback) {
        this.scheduler = scheduler;
        this.accountService = accountService;
        this.adminUser = (adminUser != null) ? adminUser : new Administrator("", "gpu2026");
        this.onSave = onSave;
        this.onLogoutCallback = onLogoutCallback;
    }

    private void triggerSave() {
        if (onSave != null) {
            onSave.run();
        }
    }

    @Override
    public SimulationClock getClock() {
        return scheduler.getClock();
    }

    @Override
    public void advanceMinute() {
        scheduler.advanceMinute();
        triggerSave();
    }

    @Override
    public GPU[] getGpus() {
        return scheduler.getGpus().toArray(new GPU[0]);
    }

    @Override
    public List<Job> getWaitingQueue() {
        return scheduler.getQueue();
    }

    @Override
    public List<Job> getAllJobs() {
        return scheduler.getJobs();
    }

    @Override
    public void cancelJob(String jobId, String cancelledBy, String reason) {
        scheduler.cancelJob(jobId, adminUser, reason);
        triggerSave();
    }

    @Override
    public List<AccessRequest> getAccessRequests() {
        return accountService.getRequests();
    }

    @Override
    public void approveRequest(AccessRequest request, int initialMinutes) {
        // Once Member 1 adds accountService.approveRequest(uniId, initialMinutes),
        // replace the two lines below with that call so the student is added to AccountService.
        request.approve();
        request.getStudent().setTimeAllowance(new TimeAllowance(initialMinutes));
        triggerSave();
    }

    @Override
    public void rejectRequest(AccessRequest request) {
        // Once Member 1 adds accountService.rejectRequest(uniId), call it here.
        request.reject();
        triggerSave();
    }

    @Override
    public List<Student> getAllStudents() {
        return accountService.getStudents();
    }

    @Override
    public Student findStudentById(String universityId) {
        return accountService.getStudent(universityId);
    }

    @Override
    public List<Job> getJobsForStudent(String universityId) {
        List<Job> result = new ArrayList<>();
        if (universityId == null) {
            return result;
        }
        for (Job job : scheduler.getJobs()) {
            if (universityId.equalsIgnoreCase(job.getStudentId())) {
                result.add(job);
            }
        }
        return result;
    }

    @Override
    public void grantExtraTime(Student student, int minutes, String reason) {
        if (student == null || student.getTimeAllowance() == null) {
            throw new IllegalStateException("Student has no active time allowance.");
        }
        SimulationClock clock = scheduler.getClock();
        if (clock == null) {
            throw new IllegalStateException("Simulation clock is not initialized.");
        }
        student.getTimeAllowance().grantTime(minutes, adminUser.getUsername(), reason, clock.getCurrentTime());
        triggerSave();
    }

    @Override
    public void onLogout() {
        triggerSave();
        if (onLogoutCallback != null) {
            onLogoutCallback.run();
        }
    }
}