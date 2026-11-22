package logic;

import java.time.LocalDateTime;
import java.util.List;

import model.GPU;
import model.Job;

public class UtilizationService {

    public static final int DEFAULT_GPU_COUNT = 4;

    public long computeTotalBusyGpuMinutes(List<Job> allJobs, LocalDateTime currentTime) {
        if (allJobs == null || currentTime == null) {
            return 0L;
        }
        long totalBusy = 0L;
        for (Job job : allJobs) {
            if (job != null) {
                // Job.getElapsedMinutes returns:
                // - 0 if startTime == null (queued or cancelled while queued)
                // - finishTime - startTime if completed or cancelled while running
                // - currentTime - startTime if currently running
                long elapsed = job.getElapsedMinutes(currentTime);
                if (elapsed > 0) {
                    totalBusy += elapsed;
                }
            }
        }
        return totalBusy;
    }

    public double computeUtilizationPercent(List<Job> allJobs, SimulationClock clock, int gpuCount) {
        if (clock == null || gpuCount <= 0) {
            return Double.NaN;
        }
        long elapsedMinutes = clock.getElapsedTime();
        if (elapsedMinutes <= 0) {
            return Double.NaN;
        }
        long totalBusyMinutes = computeTotalBusyGpuMinutes(allJobs, clock.getCurrentTime());
        double capacity = (double) gpuCount * elapsedMinutes;
        return (totalBusyMinutes / capacity) * 100.0;
    }
    
    public double computeUtilizationPercent(List<Job> allJobs, SimulationClock clock) {
        return computeUtilizationPercent(allJobs, clock, DEFAULT_GPU_COUNT);
    }

    public String formatUtilization(List<Job> allJobs, SimulationClock clock, int gpuCount) {
        double util = computeUtilizationPercent(allJobs, clock, gpuCount);
        if (Double.isNaN(util)) {
            return "N/A";
        }
        return String.format("%.1f%%", util);
    }

    public String formatUtilization(List<Job> allJobs, SimulationClock clock) {
        return formatUtilization(allJobs, clock, DEFAULT_GPU_COUNT);
    }


    public int countJobsByStatus(List<Job> allJobs, String status) {
        if (allJobs == null || status == null) {
            return 0;
        }
        int count = 0;
        for (Job job : allJobs) {
            if (job != null && status.equalsIgnoreCase(job.getStatus())) {
                count++;
            }
        }
        return count;
    }

    public int countBusyGpus(GPU[] gpus) {
        if (gpus == null) {
            return 0;
        }
        int busy = 0;
        for (GPU gpu : gpus) {
            if (gpu != null && !gpu.isFree()) {
                busy++;
            }
        }
        return busy;
    }
}
