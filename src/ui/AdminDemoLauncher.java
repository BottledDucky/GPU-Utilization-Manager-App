package ui;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import logic.PredictionService;
import logic.SimulationClock;
import logic.UtilizationService;
import model.AccessRequest;
import model.GPU;
import model.Job;
import model.Student;
import model.TimeAllowance;

public class AdminDemoLauncher {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AdminBackend demoBackend = new DemoAdminBackend();
            AdminDashboard dashboard = new AdminDashboard(
                    demoBackend,
                    new PredictionService(),
                    new UtilizationService());
            dashboard.setVisible(true);
        });
    }

    private static class DemoAdminBackend implements AdminBackend {

        private final SimulationClock clock;
        private final GPU[] gpus;
        private final List<Job> allJobs = new ArrayList<>();
        private final List<Job> queue = new ArrayList<>();
        private final List<Student> students = new ArrayList<>();
        private final List<AccessRequest> requests = new ArrayList<>();

        DemoAdminBackend() {
            LocalDateTime start = LocalDateTime.of(2026, 11, 16, 10, 0);
            clock = new SimulationClock(start);
            for (int i = 0; i < 20; i++) {
                clock.Advance(); // Advance to 10:20
            }

            // 5 approved students from the Guide's 10:20 example
            Student ali = new Student("100031025", "Ali Mansoor", "ali@ku.ac.ae", "pass", "0501112222", new TimeAllowance(100));
            Student sara = new Student("100052210", "Sara Ahmed", "sara@ku.ac.ae", "pass", "0502223333", new TimeAllowance(100));
            Student hamad = new Student("100044318", "Hamad Saeed", "hamad@ku.ac.ae", "pass", "0503334444", new TimeAllowance(100));
            Student mariam = new Student("100055001", "Mariam Al-Ali", "mariam@ku.ac.ae", "pass", "0504445555", new TimeAllowance(100));
            Student noura = new Student("100047391", "Noura Rashid", "noura@ku.ac.ae", "pass", "0505556666", new TimeAllowance(100));
            students.addAll(Arrays.asList(ali, sara, hamad, mariam, noura));

            // 2 pending registration requests (Screen 7 mockup)
            Student omar = new Student("100061477", "Omar Khalid", "omar.k@ku.ac.ae", "pass123", "0501234567", null);
            Student lina = new Student("100063052", "Lina Hassan", "lina.h@ku.ac.ae", "pass456", "0559876543", null);
            requests.add(new AccessRequest(omar));
            requests.add(new AccessRequest(lina));

            // Jobs matching Page 24 at 10:20
            Job j101 = new Job("J101", ali.getUniId(), "Notebook", "Jupyter", "Explore dataset", "SMALL", start);
            ali.getTimeAllowance().reserve("J101", 5);
            j101.start(1, start);
            j101.complete(start.plusMinutes(5));
            ali.getTimeAllowance().complete("J101");

            Job j102 = new Job("J102", sara.getUniId(), "Data cleaning", "Pandas", "Clean CSV", "MEDIUM", start);
            sara.getTimeAllowance().reserve("J102", 15);
            j102.start(2, start);
            j102.complete(start.plusMinutes(15));
            sara.getTimeAllowance().complete("J102");

            Job j103 = new Job("J103", hamad.getUniId(), "Genome alignment", "BLAST", "Align reads", "MEDIUM", start);
            hamad.getTimeAllowance().reserve("J103", 15);
            j103.start(3, start);
            j103.complete(start.plusMinutes(15));
            hamad.getTimeAllowance().complete("J103");

            Job j104 = new Job("J104", mariam.getUniId(), "Weather model", "PyTorch", "Train CNN", "LARGE", start);
            mariam.getTimeAllowance().reserve("J104", 30);
            j104.start(4, start);

            Job j105 = new Job("J105", ali.getUniId(), "ResNet run", "PyTorch", "Train ResNet", "LARGE", start.plusMinutes(8));
            ali.getTimeAllowance().reserve("J105", 30);
            j105.start(1, start.plusMinutes(8));

            Job j106 = new Job("J106", sara.getUniId(), "CNN training", "PyTorch", "Radar CNN", "MEDIUM", start.plusMinutes(10));
            sara.getTimeAllowance().reserve("J106", 15);
            j106.start(2, start.plusMinutes(15));

            Job j107 = new Job("J107", hamad.getUniId(), "Protein folding", "AlphaFold", "Fold sample", "LARGE", start.plusMinutes(12));
            hamad.getTimeAllowance().reserve("J107", 30);
            j107.start(3, start.plusMinutes(15));

            Job j108 = new Job("J108", noura.getUniId(), "Classifier", "TensorFlow", "Train model", "MEDIUM", start.plusMinutes(16));
            noura.getTimeAllowance().reserve("J108", 15);

            Job j109 = new Job("J109", sara.getUniId(), "Image resize", "OpenCV", "Resize batch", "SMALL", start.plusMinutes(18));
            sara.getTimeAllowance().reserve("J109", 5);

            Job j110 = new Job("J110", hamad.getUniId(), "Quick test", "Python", "Sanity check", "SMALL", start.plusMinutes(19));
            hamad.getTimeAllowance().reserve("J110", 5);
            j110.cancel(start.plusMinutes(19), hamad.getUniId(), "Changed mind");
            hamad.getTimeAllowance().release("J110");

            allJobs.addAll(Arrays.asList(j101, j102, j103, j104, j105, j106, j107, j108, j109, j110));
            queue.addAll(Arrays.asList(j108, j109));

            gpus = new GPU[] {
                    new GPU(1, j105),
                    new GPU(2, j106),
                    new GPU(3, j107),
                    new GPU(4, j104)
            };
        }

        @Override
        public SimulationClock getClock() { return clock; }

        @Override
        public void advanceMinute() {
            clock.Advance();
            LocalDateTime now = clock.getCurrentTime();

            // Complete finished running jobs
            for (GPU gpu : gpus) {
                if (!gpu.isFree()) {
                    Job job = gpu.getCurrentJob();
                    if (job.getRemainingMinutes(now) <= 0) {
                        job.complete(now);
                        Student owner = findStudentById(job.getStudentId());
                        if (owner != null && owner.getTimeAllowance() != null) {
                            owner.getTimeAllowance().complete(job.getJobId());
                        }
                        gpu.free();
                    }
                }
            }
            dispatchQueuedJobs();
        }

        private void dispatchQueuedJobs() {
            LocalDateTime now = clock.getCurrentTime();
            for (GPU gpu : gpus) {
                if (gpu.isFree() && !queue.isEmpty()) {
                    Job next = queue.remove(0);
                    next.start(gpu.getGpuId(), now);
                    gpu.assign(next);
                }
            }
        }

        @Override
        public GPU[] getGpus() { return gpus; }

        @Override
        public List<Job> getWaitingQueue() { return new ArrayList<>(queue); }

        @Override
        public List<Job> getAllJobs() { return new ArrayList<>(allJobs); }

        @Override
        public void cancelJob(String jobId, String cancelledBy, String reason) {
            Job target = null;
            for (Job j : allJobs) {
                if (j.getJobId().equals(jobId)) {
                    target = j;
                    break;
                }
            }
            if (target == null) {
                throw new IllegalArgumentException("Job not found: " + jobId);
            }
            LocalDateTime now = clock.getCurrentTime();
            Student owner = findStudentById(target.getStudentId());

            if ("QUEUED".equalsIgnoreCase(target.getStatus())) {
                queue.remove(target);
                target.cancel(now, cancelledBy, reason);
                if (owner != null && owner.getTimeAllowance() != null) {
                    owner.getTimeAllowance().release(target.getJobId());
                }
            } else if ("RUNNING".equalsIgnoreCase(target.getStatus())) {
                int elapsed = (int) target.getElapsedMinutes(now);
                target.cancel(now, cancelledBy, reason);
                if (owner != null && owner.getTimeAllowance() != null) {
                    owner.getTimeAllowance().charge(target.getJobId(), elapsed);
                }
                for (GPU gpu : gpus) {
                    if (!gpu.isFree() && gpu.getCurrentJob() == target) {
                        gpu.free();
                    }
                }
                dispatchQueuedJobs();
            }
        }

        @Override
        public List<AccessRequest> getAccessRequests() { return new ArrayList<>(requests); }

        @Override
        public void approveRequest(AccessRequest request, int initialMinutes) {
            request.approve();
            Student s = request.getStudent();
            s.setTimeAllowance(new TimeAllowance(initialMinutes));
            students.add(s);
        }

        @Override
        public void rejectRequest(AccessRequest request) {
            request.reject();
        }

        @Override
        public List<Student> getAllStudents() { return new ArrayList<>(students); }

        @Override
        public Student findStudentById(String universityId) {
            for (Student s : students) {
                if (s.getUniId().equalsIgnoreCase(universityId)) {
                    return s;
                }
            }
            return null;
        }

        @Override
        public List<Job> getJobsForStudent(String universityId) {
            List<Job> result = new ArrayList<>();
            for (Job j : allJobs) {
                if (j.getStudentId().equalsIgnoreCase(universityId)) {
                    result.add(j);
                }
            }
            return result;
        }

        @Override
        public void grantExtraTime(Student student, int minutes, String reason) {
            student.getTimeAllowance().grantTime(minutes, "admin", reason, clock.getCurrentTime());
        }

        @Override
        public void onLogout() {
            JOptionPane.showMessageDialog(null, "Logged out (in the full app, this opens LoginFrame).");
        }
    }
}
