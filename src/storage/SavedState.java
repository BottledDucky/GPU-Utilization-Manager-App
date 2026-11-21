package storage;
import java.io.*;
import java.time.*;
import java.util.*;
import model.*;

public final class SavedState implements Serializable {
    private static final int serialVersionUID = 1;
    private LocalDateTime clockStart;
    private LocalDateTime clockNow;
    private List<StudentData> students = new ArrayList<>();
    private List<RequestData> requests = new ArrayList<>();
    private List<JobData> jobs = new ArrayList<>();
    private List<String> queueJobIds = new ArrayList<>();
    private Map<Integer, String> gpuAssignments = new LinkedHashMap<>();
    private int nextJobNumber = 1;

    public SavedState() {
    }

    public void setClock(SimulationClock clock) {
        if (clock == null) {
            throw new IllegalArgumentException("Clock is required.");
        }
        clockStart = clock.getStartTime();
        clockNow = clock.getCurrentTime();
    }
    public void setClock(LocalDateTime start, LocalDateTime now) {
        clockStart = start;
        clockNow = now;
    }

    public LocalDateTime getClockStart() {
        return clockStart;
    }

    public LocalDateTime getClockNow() {
        return clockNow;
    }

    public void addStudent(Student student) {
        students.add(new StudentData(student));
    }

    public void addRequest(AccessRequest request) {
        requests.add(new RequestData(request));
    }
    public void addJob(Job job) {
        jobs.add(new JobData(job));
    }
    public void enqueue(String jobId) {
        queueJobIds.add(jobId);
    }
    public void assignGpu(int gpuId, String jobId) {
        if (gpuId < 1 || gpuId > 4) {
            throw new IllegalArgumentException("GPU ID must be 1 to 4.");
        }
        gpuAssignments.put(gpuId, jobId);
    }
    public void setNextJobNumber(int number) {
        nextJobNumber = number;
    }
    public int getNextJobNumber() {
        return nextJobNumber;
    }
    public List<String> getQueueJobIds() {
        return new ArrayList<>(queueJobIds);
    }
    public Map<Integer, String> getGpuAssignments() {
        return new LinkedHashMap<>(gpuAssignments);
    }
    public Map<String, Student> restoreStudents() {
        Map<String, Student> result = new LinkedHashMap<>();
        for (StudentData data : students) {
            Student student = data.toStudent();
            result.put(student.getUsername(), student);
        }
        return result;
    }
    public List<AccessRequest> restoreRequests() {
        List<AccessRequest> result = new ArrayList<>();
        for (RequestData data : requests) {
            result.add(data.toRequest());
        }
        return result;
    }
    public List<Job> restoreJobs() {
        List<Job> result = new ArrayList<>();
        for (JobData data : jobs) {
            result.add(data.toJob());
        }
        return result;
    }
    public SimulationClock restoreClock() {
        if (clockStart == null) {
            return null;
        }
        SimulationClock clock = new SimulationClock(clockStart);
        int elapsed = Math.toIntExact(
        	    java.time.Duration.between(clockStart, clockNow).toMinutes()
        	);

        	for (int i = 0; i < elapsed; i++) {
        	    clock.Advance();
        	}
        return clock;
    }
    public void validate() {

        if ((clockStart == null) != (clockNow == null)) {
            throw new IllegalArgumentException("Incomplete clock data.");
        }

        if (clockStart != null && clockNow.isBefore(clockStart)) {
            throw new IllegalArgumentException("Invalid simulation clock.");
        }

        if (nextJobNumber < 101) {
            throw new IllegalArgumentException("Invalid next job number.");
        }

        if (students == null || requests == null || jobs == null|| queueJobIds == null || gpuAssignments == null) {
            throw new IllegalArgumentException("Missing saved data.");
        }
        Set<String> studentIds = new HashSet<>();
        for (StudentData student : students) {
            Student restored = student.toStudent();
            if (!studentIds.add(restored.getUsername())) {
                throw new IllegalArgumentException("Duplicate student ID.");
            }
        }

        Map<String, JobData> jobMap = new HashMap<>();

        for (JobData job : jobs) {
            if (job == null || job.jobId == null|| jobMap.containsKey(job.jobId)) {
                throw new IllegalArgumentException("Invalid or duplicate job.");
            }
            if (clockNow == null || job.submissionTime == null|| job.submissionTime.isAfter(clockNow)) {
                throw new IllegalArgumentException("Invalid job timestamp.");
            }
            job.toJob();
            jobMap.put(job.jobId, job);
        }

        Set<String> queuedIds = new HashSet<>();

        for (String id : queueJobIds) {
            JobData job = jobMap.get(id);

            if (job == null || !job.status.equals("QUEUED")|| !queuedIds.add(id)) {
                throw new IllegalArgumentException("Invalid queue information.");
            }
        }

        Set<String> runningIds = new HashSet<>();

        for (Map.Entry<Integer, String> entry : gpuAssignments.entrySet()) {
            Integer gpuId = entry.getKey();
            JobData job = jobMap.get(entry.getValue());

            if (gpuId == null || gpuId < 1 || gpuId > 4|| job == null || !job.status.equals("RUNNING")|| !gpuId.equals(job.gpuId)|| !runningIds.add(job.jobId)) {
                throw new IllegalArgumentException("Invalid GPU assignment.");
            }
        }

        for (JobData job : jobs) {
            if (job.status.equals("QUEUED")&& !queuedIds.contains(job.jobId)) {
                throw new IllegalArgumentException("Queued job missing from queue.");
            }

            if (job.status.equals("RUNNING")&& !runningIds.contains(job.jobId)) {
                throw new IllegalArgumentException("Running job missing from GPU.");
            }
        }

        for (RequestData request : requests) {
            request.toRequest();
        }
    }

    private static class StudentData implements Serializable {

        private static final int serialVersionUID = 1;

        private String username;
        private String name;
        private String email;
        private String passwordHash;
        private String phone;

        private boolean hasAllowance;
        private int allocated;
        private int used;

        private Map<String, Integer> reservations;
        private Set<String> settledJobs;
        private List<TimeGrant> grants;

        StudentData(Student student) {

            username = student.getUsername();
            name = student.getName();
            email = student.getEmail();
            passwordHash = student.getPassHash();
            phone = student.getPhoneNum();

            TimeAllowance allowance = student.getTimeAllowance();

            hasAllowance = allowance != null;

            if (hasAllowance) {
                allocated = allowance.getAllocated();
                used = allowance.getUsed();
                reservations = allowance.getReservations();
                settledJobs = allowance.getSettledJobIds();
                grants = allowance.getGrants();
            }
        }

        Student toStudent() {

            TimeAllowance allowance = null;

            if (hasAllowance) {
                allowance = TimeAllowance.restore(
                    allocated, used, reservations, settledJobs, grants);
            }

            return new Student(username, name, email,
                    passwordHash, phone, allowance);
        }
    }
    private static class RequestData implements Serializable {

        private static final int serialVersionUID = 1;

        private StudentData student;
        private String status;

        RequestData(AccessRequest request) {
            student = new StudentData(request.getStudent());
            status = request.getStatus();
        }

        AccessRequest toRequest() {

            AccessRequest request = new AccessRequest(student.toStudent());

            if ("APPROVED".equals(status)) {
                request.approve();
            } else if ("REJECTED".equals(status)) {
                request.reject();
            } else if (!"PENDING".equals(status)) {
                throw new IllegalArgumentException("Invalid request status.");
            }

            return request;
        }
    }

    private static class JobData implements Serializable {
        private static final int serialVersionUID = 1;
        private String jobId;
        private String studentId;
        private String title;
        private String application;
        private String description;
        private String taskSize;
        private String status;
        private Integer gpuId;
        private LocalDateTime submissionTime;
        private LocalDateTime startTime;
        private LocalDateTime finishTime;
        private LocalDateTime cancellationTime;
        private String cancelledBy;
        private String cancellationReason;
        JobData(Job job) {
            jobId = job.getJobId();
            studentId = job.getStudentId();
            title = job.getTitle();
            application = job.getApplicationName();
            description = job.getDescription();
            taskSize = job.getTaskSize();
            status = job.getStatus();
            gpuId = job.getAssignedGpuID();
            submissionTime = job.getSubmissionTime();
            startTime = job.getStartTime();
            finishTime = job.getFinishTime();
            cancellationTime = job.getCancellationTime();
            cancelledBy = job.getCancelledBy();
            cancellationReason = job.getCancellationReason();
        }

        Job toJob() {

            Job job = new Job(jobId, studentId, title,application, description, taskSize, submissionTime);
            if ("QUEUED".equals(status)) {
                if (gpuId != null || startTime != null || finishTime != null) {
                    throw new IllegalArgumentException("Invalid queued job.");
                }
                return job;
            }

            if ("CANCELLED".equals(status) && startTime == null) {
                if (gpuId != null || finishTime != null|| cancellationTime == null) {
                    throw new IllegalArgumentException("Invalid queued cancellation.");
                }

                job.cancel(cancellationTime, cancelledBy, cancellationReason);
                return job;
            }

            if (gpuId == null || startTime == null) {
                throw new IllegalArgumentException("Missing job start information.");
            }

            job.start(gpuId, startTime);

            if ("COMPLETED".equals(status)) {
                if (finishTime == null) {
                    throw new IllegalArgumentException("Missing finish time.");
                }
                job.complete(finishTime);

            } else if ("CANCELLED".equals(status)) {
                if (cancellationTime == null) {
                    throw new IllegalArgumentException("Missing cancellation time.");
                }
                job.cancel(cancellationTime, cancelledBy, cancellationReason);

            } else if (!"RUNNING".equals(status)) {
                throw new IllegalArgumentException("Invalid job status.");
            }

            return job;
        }
    }
}
