package logic;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import model.GPU;
import model.Job;

public class PredictionService {

    /**
     * Immutable forecast result for a single queued job.
     */
    public static class JobPrediction {
        private final String jobId;
        private final int queuePosition;
        private final int predictedGpuId;
        private final LocalDateTime predictedStart;
        private final LocalDateTime predictedFinish;
        private final long remainingWaitMinutes;

        public JobPrediction(
                String jobId,
                int queuePosition,
                int predictedGpuId,
                LocalDateTime predictedStart,
                LocalDateTime predictedFinish,
                long remainingWaitMinutes) {
            this.jobId = jobId;
            this.queuePosition = queuePosition;
            this.predictedGpuId = predictedGpuId;
            this.predictedStart = predictedStart;
            this.predictedFinish = predictedFinish;
            this.remainingWaitMinutes = remainingWaitMinutes;
        }

        public String getJobId() {
            return jobId;
        }

        public int getQueuePosition() {
            return queuePosition;
        }

        public int getPredictedGpuId() {
            return predictedGpuId;
        }

        public LocalDateTime getPredictedStart() {
            return predictedStart;
        }

        public LocalDateTime getPredictedFinish() {
            return predictedFinish;
        }

        public long getRemainingWaitMinutes() {
            return remainingWaitMinutes;
        }
    }
    public Map<String, JobPrediction> computePredictions(
            GPU[] gpus,
            List<Job> queuedJobs,
            LocalDateTime currentTime) {

        Map<String, JobPrediction> results = new LinkedHashMap<>();

        if (gpus == null || gpus.length == 0 || queuedJobs == null || queuedJobs.isEmpty() || currentTime == null) {
            return results;
        }

        // Step 1: Copy and sort GPU references by ascending GPU ID so tie-breaking favors lower GPU ID.
        GPU[] sortedGpus = Arrays.copyOf(gpus, gpus.length);
        Arrays.sort(sortedGpus, Comparator.comparingInt(g -> g.getGpuId() != null ? g.getGpuId() : Integer.MAX_VALUE));

        // Create a COPY of each GPU's "free at" time.
        LocalDateTime[] freeAt = new LocalDateTime[sortedGpus.length];
        for (int i = 0; i < sortedGpus.length; i++) {
            GPU gpu = sortedGpus[i];
            if (gpu == null || gpu.isFree() || gpu.getCurrentJob() == null) {
                freeAt[i] = currentTime;
            } else {
                LocalDateTime expectedFinish = gpu.getCurrentJob().getExpectedFinishTime();
                if (expectedFinish == null || expectedFinish.isBefore(currentTime)) {
                    freeAt[i] = currentTime;
                } else {
                    freeAt[i] = expectedFinish;
                }
            }
        }

        // Step 2: Copy queued jobs and sort in FCFS order (submissionTime ascending, then jobId ascending).
        List<Job> fcfsQueue = new ArrayList<>();
        for (Job job : queuedJobs) {
            if (job != null && "QUEUED".equalsIgnoreCase(job.getStatus())) {
                fcfsQueue.add(job);
            }
        }
        fcfsQueue.sort(this::compareFcfs);

        // Step 3 & 4: Simulate assigning each queued job to the earliest free GPU copy.
        int position = 1;
        for (Job job : fcfsQueue) {
            int earliestIndex = findEarliestGpuIndex(freeAt);
            LocalDateTime predictedStart = freeAt[earliestIndex];
            LocalDateTime predictedFinish = predictedStart.plusMinutes(job.getEstimatedDuration());
            long waitMinutes = Math.max(0, Duration.between(currentTime, predictedStart).toMinutes());

            int gpuId = (sortedGpus[earliestIndex] != null && sortedGpus[earliestIndex].getGpuId() != null)
                    ? sortedGpus[earliestIndex].getGpuId()
                    : (earliestIndex + 1);

            results.put(job.getJobId(), new JobPrediction(
                    job.getJobId(),
                    position,
                    gpuId,
                    predictedStart,
                    predictedFinish,
                    waitMinutes));

            // Update ONLY the local copy of freeAt[earliestIndex].
            freeAt[earliestIndex] = predictedFinish;
            position++;
        }

        return results;
    }

    /**
     * Convenience overload that accepts a SimulationClock directly.
     */
    public Map<String, JobPrediction> computePredictions(
            GPU[] gpus,
            List<Job> queuedJobs,
            SimulationClock clock) {
        if (clock == null) {
            return new LinkedHashMap<>();
        }
        return computePredictions(gpus, queuedJobs, clock.getCurrentTime());
    }

    /**
     * Finds the index of the GPU with the earliest free-at time.
     * Strict isBefore() preserves tie-breaking by lowest GPU ID (since sortedGpus is ordered by ID).
     */
    private int findEarliestGpuIndex(LocalDateTime[] freeAt) {
        int best = 0;
        for (int i = 1; i < freeAt.length; i++) {
            if (freeAt[i].isBefore(freeAt[best])) {
                best = i;
            }
        }
        return best;
    }

    /**
     * FCFS comparator: earliest submissionTime first, then lowest jobId.
     * Handles numeric suffixes cleanly (e.g., J9 comes before J10, J101 before J102).
     */
    private int compareFcfs(Job a, Job b) {
        LocalDateTime timeA = a.getSubmissionTime();
        LocalDateTime timeB = b.getSubmissionTime();
        if (timeA != null && timeB != null) {
            int cmp = timeA.compareTo(timeB);
            if (cmp != 0) {
                return cmp;
            }
        } else if (timeA != null) {
            return -1;
        } else if (timeB != null) {
            return 1;
        }
        return compareJobIds(a.getJobId(), b.getJobId());
    }

    private int compareJobIds(String idA, String idB) {
        if (idA == null && idB == null) return 0;
        if (idA == null) return -1;
        if (idB == null) return 1;

        Integer numA = extractTrailingNumber(idA);
        Integer numB = extractTrailingNumber(idB);
        if (numA != null && numB != null) {
            int numCmp = Integer.compare(numA, numB);
            if (numCmp != 0) {
                return numCmp;
            }
        }
        return idA.compareToIgnoreCase(idB);
    }

    private Integer extractTrailingNumber(String id) {
        int i = id.length() - 1;
        while (i >= 0 && Character.isDigit(id.charAt(i))) {
            i--;
        }
        if (i == id.length() - 1) {
            return null;
        }
        try {
            return Integer.parseInt(id.substring(i + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
