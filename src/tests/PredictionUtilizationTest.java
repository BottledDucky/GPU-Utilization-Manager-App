package tests;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import logic.PredictionService;
import logic.SimulationClock;
import logic.UtilizationService;
import model.GPU;
import model.Job;

public class PredictionUtilizationTest {

    public static void main(String[] args) {
        PredictionService predictionService = new PredictionService();
        UtilizationService utilizationService = new UtilizationService();

        // 1. Test Time Zero -> Utilization must be "N/A"
        LocalDateTime start = LocalDateTime.of(2026, 11, 16, 10, 0);
        SimulationClock clock = new SimulationClock(start);
        String zeroUtil = utilizationService.formatUtilization(new ArrayList<>(), clock);
        assertEquals("Time-zero utilization", "N/A", zeroUtil);

        // 2. Advance clock to 10:20 (20 minutes elapsed)
        for (int i = 0; i < 20; i++) {
            clock.Advance();
        }
        assertEquals("Elapsed minutes at 10:20", 20L, clock.getElapsedTime());

        // 3. Recreate the exact jobs from Page 24 of the Lab Project Guide
        List<Job> allJobs = new ArrayList<>();

        // Completed jobs
        Job j101 = new Job("J101", "100031025", "Notebook", "Jupyter", "Desc", "SMALL", start);
        j101.start(1, start);
        j101.complete(start.plusMinutes(5)); // 10:00 - 10:05 (5 min)
        allJobs.add(j101);

        Job j102 = new Job("J102", "100052210", "Data cleaning", "Pandas", "Desc", "MEDIUM", start);
        j102.start(2, start);
        j102.complete(start.plusMinutes(15)); // 10:00 - 10:15 (15 min)
        allJobs.add(j102);

        Job j103 = new Job("J103", "100044318", "Genome align", "BLAST", "Desc", "MEDIUM", start);
        j103.start(3, start);
        j103.complete(start.plusMinutes(15)); // 10:00 - 10:15 (15 min)
        allJobs.add(j103);

        // Running jobs at 10:20
        Job j104 = new Job("J104", "100055001", "Model run", "PyTorch", "Desc", "LARGE", start);
        j104.start(4, start); // 10:00 - 10:30 (20 min elapsed at 10:20)
        allJobs.add(j104);

        Job j105 = new Job("J105", "100031025", "Training", "PyTorch", "Desc", "LARGE", start.plusMinutes(8));
        j105.start(1, start.plusMinutes(8)); // 10:08 - 10:38 (12 min elapsed at 10:20)
        allJobs.add(j105);

        Job j106 = new Job("J106", "100052210", "CNN training", "PyTorch", "Desc", "MEDIUM", start.plusMinutes(10));
        j106.start(2, start.plusMinutes(15)); // 10:15 - 10:30 (5 min elapsed at 10:20)
        allJobs.add(j106);

        Job j107 = new Job("J107", "100044318", "Protein fold", "AlphaFold", "Desc", "LARGE", start.plusMinutes(12));
        j107.start(3, start.plusMinutes(15)); // 10:15 - 10:45 (5 min elapsed at 10:20)
        allJobs.add(j107);

        // Queued jobs at 10:20
        Job j108 = new Job("J108", "100047391", "Classifier", "TensorFlow", "Desc", "MEDIUM", start.plusMinutes(16));
        Job j109 = new Job("J109", "100052210", "Image resize", "OpenCV", "Desc", "SMALL", start.plusMinutes(18));
        Job j111 = new Job("J111", "100055001", "Extra batch", "PyTorch", "Desc", "LARGE", start.plusMinutes(20));

        // Cancelled while queued at 10:19 (0 busy minutes)
        Job j110 = new Job("J110", "100044318", "Quick test", "Python", "Desc", "SMALL", start.plusMinutes(19));
        j110.cancel(start.plusMinutes(19), "100044318", "No longer needed");

        allJobs.addAll(Arrays.asList(j108, j109, j110));

        // 4 GPUs at 10:20
        GPU[] gpus = new GPU[] {
                new GPU(1, j105), // finishes 10:38
                new GPU(2, j106), // finishes 10:30
                new GPU(3, j107), // finishes 10:45
                new GPU(4, j104)  // finishes 10:30
        };

        // 4. Verify Historical Utilization = 77 / 80 * 100 = 96.3%
        long busyMinutes = utilizationService.computeTotalBusyGpuMinutes(allJobs, clock.getCurrentTime());
        assertEquals("Total busy GPU minutes at 10:20", 77L, busyMinutes);
        String utilFormatted = utilizationService.formatUtilization(allJobs, clock);
        assertEquals("Formatted utilization at 10:20", "96.3%", utilFormatted);

        // 5. Verify Predictions for J108, J109, J111 (Page 25)
        List<Job> queue = new ArrayList<>(Arrays.asList(j108, j109, j111));
        Map<String, PredictionService.JobPrediction> preds =
                predictionService.computePredictions(gpus, queue, clock);

        PredictionService.JobPrediction p108 = preds.get("J108");
        assertEquals("J108 GPU (tie GPU2 vs GPU4 -> GPU2)", 2, p108.getPredictedGpuId());
        assertEquals("J108 start", LocalDateTime.of(2026, 11, 16, 10, 30), p108.getPredictedStart());
        assertEquals("J108 finish", LocalDateTime.of(2026, 11, 16, 10, 45), p108.getPredictedFinish());
        assertEquals("J108 wait", 10L, p108.getRemainingWaitMinutes());

        PredictionService.JobPrediction p109 = preds.get("J109");
        assertEquals("J109 GPU", 4, p109.getPredictedGpuId());
        assertEquals("J109 start", LocalDateTime.of(2026, 11, 16, 10, 30), p109.getPredictedStart());
        assertEquals("J109 finish", LocalDateTime.of(2026, 11, 16, 10, 35), p109.getPredictedFinish());
        assertEquals("J109 wait", 10L, p109.getRemainingWaitMinutes());

        PredictionService.JobPrediction p111 = preds.get("J111");
        assertEquals("J111 GPU (10:35 < GPU1 10:38 -> GPU4 again)", 4, p111.getPredictedGpuId());
        assertEquals("J111 start", LocalDateTime.of(2026, 11, 16, 10, 35), p111.getPredictedStart());
        assertEquals("J111 finish", LocalDateTime.of(2026, 11, 16, 11, 5), p111.getPredictedFinish());
        assertEquals("J111 wait", 15L, p111.getRemainingWaitMinutes());

        // 6. Verify live GPUs were NOT modified by computePredictions()
        assertEquals("Live GPU1 still runs J105", "J105", gpus[0].getCurrentJob().getJobId());
        assertEquals("Live GPU2 still runs J106", "J106", gpus[1].getCurrentJob().getJobId());
        assertEquals("Live GPU3 still runs J107", "J107", gpus[2].getCurrentJob().getJobId());
        assertEquals("Live GPU4 still runs J104", "J104", gpus[3].getCurrentJob().getJobId());

        System.out.println("===================================================");
        System.out.println("ALL MEMBER 3 PREDICTION & UTILIZATION TESTS PASSED!");
        System.out.println("Utilization at 10:20 : " + utilFormatted + " (77 / 80)");
        System.out.println("J108 -> GPU" + p108.getPredictedGpuId() + " start " + p108.getPredictedStart() + " wait " + p108.getRemainingWaitMinutes() + " min");
        System.out.println("J109 -> GPU" + p109.getPredictedGpuId() + " start " + p109.getPredictedStart() + " wait " + p109.getRemainingWaitMinutes() + " min");
        System.out.println("J111 -> GPU" + p111.getPredictedGpuId() + " start " + p111.getPredictedStart() + " wait " + p111.getRemainingWaitMinutes() + " min");
        System.out.println("===================================================");
    }

    private static void assertEquals(String label, Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + " FAILED! Expected: " + expected + ", Actual: " + actual);
        }
    }
}
