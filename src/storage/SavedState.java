package storage;
import logic.SimulationClock;
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
            String id=restored.getUsername();
            if(studentIds.contains(id)) throw new IllegalArgumentException("Duplicate student ID.");
            studentIds.add(id);
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


    // Store each value as a readable key=value entry in the text file.
    private static void put(Properties data, String key, Object value) {
        if (value != null) data.setProperty(key, value.toString());
    }
    private static String get(Properties data, String key) throws IOException {
        String value = data.getProperty(key);
        if (value == null) throw new IOException("Missing saved field: " + key);
        return value;
    }
    private static int number(Properties data, String key) throws IOException {
        return Integer.parseInt(get(data, key));
    }
    private static LocalDateTime time(Properties data, String key) {
        String value = data.getProperty(key);
        return value == null ? null : LocalDateTime.parse(value);
    }
    private static void saveStudent(Properties data, String key, StudentData student) {
        put(data, key + ".username", student.username);
        put(data, key + ".name", student.name);
        put(data, key + ".email", student.email);
        put(data, key + ".passwordHash", student.passwordHash);
        put(data, key + ".phone", student.phone);
        put(data, key + ".hasAllowance", student.hasAllowance);
        if (!student.hasAllowance) return;
        put(data, key + ".allocated", student.allocated);
        put(data, key + ".used", student.used);
        put(data, key + ".reservations.count", student.reservations.size());
        int i = 0;
        for (Map.Entry<String, Integer> entry : student.reservations.entrySet()) {
            put(data, key + ".reservation." + i + ".job", entry.getKey());
            put(data, key + ".reservation." + i + ".minutes", entry.getValue());
            i++;
        }
        put(data, key + ".settled.count", student.settledJobs.size());
        i = 0;
        for (String id : student.settledJobs) put(data, key + ".settled." + i++, id);
        put(data, key + ".grants.count", student.grants.size());
        for (i = 0; i < student.grants.size(); i++) {
            TimeGrant grant = student.grants.get(i);
            String g = key + ".grant." + i;
            put(data, g + ".minutes", grant.getMinutes());
            put(data, g + ".admin", grant.getAdminUsername());
            put(data, g + ".reason", grant.getReason());
            put(data, g + ".time", grant.getSimulatedTime());
        }
    }
    private static StudentData loadStudent(Properties data, String key) throws IOException {
        StudentData student = new StudentData(new Student(
            get(data, key + ".username"), get(data, key + ".name"),
            get(data, key + ".email"), get(data, key + ".passwordHash"),
            get(data, key + ".phone"), null));
        student.hasAllowance = Boolean.parseBoolean(get(data, key + ".hasAllowance"));
        if (!student.hasAllowance) return student;
        student.allocated = number(data, key + ".allocated");
        student.used = number(data, key + ".used");
        student.reservations = new LinkedHashMap<>();
        for (int i = 0; i < number(data, key + ".reservations.count"); i++) {
            student.reservations.put(get(data, key + ".reservation." + i + ".job"),
                number(data, key + ".reservation." + i + ".minutes"));
        }
        student.settledJobs = new LinkedHashSet<>();
        for (int i = 0; i < number(data, key + ".settled.count"); i++)
            student.settledJobs.add(get(data, key + ".settled." + i));
        student.grants = new ArrayList<>();
        for (int i = 0; i < number(data, key + ".grants.count"); i++) {
            String g = key + ".grant." + i;
            student.grants.add(new TimeGrant(number(data, g + ".minutes"),
                get(data, g + ".admin"), get(data, g + ".reason"), time(data, g + ".time")));
        }
        return student;
    }
    public void writeText(PrintWriter output) throws IOException {
        validate();
        Properties data = new Properties();
        put(data, "format", "GPU_STATE_TEXT_V1");
        put(data, "clockStart", clockStart);
        put(data, "clockNow", clockNow);
        put(data, "nextJobNumber", nextJobNumber);
        put(data, "students.count", students.size());
        for (int i = 0; i < students.size(); i++) saveStudent(data, "student." + i, students.get(i));
        put(data, "requests.count", requests.size());
        for (int i = 0; i < requests.size(); i++) {
            saveStudent(data, "request." + i + ".student", requests.get(i).student);
            put(data, "request." + i + ".status", requests.get(i).status);
        }
        put(data, "jobs.count", jobs.size());
        for (int i = 0; i < jobs.size(); i++) {
            JobData job = jobs.get(i);
            String key = "job." + i + ".";
            String[] names = {"jobId", "studentId", "title", "application", "description", "taskSize", "status", "gpuId", "submissionTime", "startTime", "finishTime", "cancellationTime", "cancelledBy", "cancellationReason"};
            Object[] values = {job.jobId, job.studentId, job.title, job.application, job.description, job.taskSize, job.status, job.gpuId, job.submissionTime, job.startTime, job.finishTime, job.cancellationTime, job.cancelledBy, job.cancellationReason};
            for (int j = 0; j < names.length; j++) put(data, key + names[j], values[j]);
        }
        put(data, "queue.count", queueJobIds.size());
        for (int i = 0; i < queueJobIds.size(); i++) put(data, "queue." + i, queueJobIds.get(i));
        put(data, "gpus.count", gpuAssignments.size());
        int i = 0;
        for (Map.Entry<Integer, String> entry : gpuAssignments.entrySet()) {
            put(data, "gpu." + i + ".id", entry.getKey());
            put(data, "gpu." + i + ".job", entry.getValue());
            i++;
        }
        // Properties escapes newlines and separators so descriptions are preserved.
        data.store(output, "GPU simulation text save");
    }
    public static SavedState readText(Scanner input) throws IOException {
        Properties data = new Properties();
        StringBuilder text = new StringBuilder();
        while (input.hasNextLine()) text.append(input.nextLine()).append('\n');
        data.load(new StringReader(text.toString()));
        if (!"GPU_STATE_TEXT_V1".equals(data.getProperty("format")))
            throw new IOException("Invalid save file header.");
        try {
            SavedState state = new SavedState();
            state.clockStart = time(data, "clockStart");
            state.clockNow = time(data, "clockNow");
            state.nextJobNumber = number(data, "nextJobNumber");
            for (int i = 0; i < number(data, "students.count"); i++)
                state.students.add(loadStudent(data, "student." + i));
            for (int i = 0; i < number(data, "requests.count"); i++) {
                String key = "request." + i;
                RequestData request = new RequestData(new AccessRequest(
                    loadStudent(data, key + ".student").toStudent()));
                request.status = get(data, key + ".status");
                state.requests.add(request);
            }
            for (int i = 0; i < number(data, "jobs.count"); i++) {
                String key = "job." + i + ".";
                JobData job = new JobData(new Job(get(data, key + "jobId"),
                    get(data, key + "studentId"), get(data, key + "title"),
                    get(data, key + "application"), get(data, key + "description"),
                    get(data, key + "taskSize"), time(data, key + "submissionTime")));
                job.status = get(data, key + "status");
                String gpu = data.getProperty(key + "gpuId");
                job.gpuId = gpu == null ? null : Integer.parseInt(gpu);
                job.startTime = time(data, key + "startTime");
                job.finishTime = time(data, key + "finishTime");
                job.cancellationTime = time(data, key + "cancellationTime");
                job.cancelledBy = data.getProperty(key + "cancelledBy");
                job.cancellationReason = data.getProperty(key + "cancellationReason");
                state.jobs.add(job);
            }
            for (int i = 0; i < number(data, "queue.count"); i++)
                state.queueJobIds.add(get(data, "queue." + i));
            for (int i = 0; i < number(data, "gpus.count"); i++)
                state.gpuAssignments.put(number(data, "gpu." + i + ".id"),
                    get(data, "gpu." + i + ".job"));
            state.validate();
            return state;
        } catch (RuntimeException e) {
            throw new IOException("Invalid saved simulation data: " + e.getMessage(), e);
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
