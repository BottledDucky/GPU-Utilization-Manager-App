package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import logic.PredictionService;
import logic.SimulationClock;
import logic.UtilizationService;
import model.AccessRequest;
import model.GPU;
import model.Job;
import model.Student;
import model.TimeAllowance;

public class AdminDashboard extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FULL_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter SHORT_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final AdminBackend backend;
    private final PredictionService predictionService;
    private final UtilizationService utilizationService;

    // Header components
    private final JLabel clockLabel;
    private final JButton advanceMinuteButton;

    // Tabs
    private final JTabbedPane tabbedPane;
    private final RequestsPanel requestsPanel;
    private final StudentsPanel studentsPanel;
    private final AllJobsPanel allJobsPanel;

    // Dashboard KPI value labels
    private final JLabel runningCountLabel = new JLabel("0");
    private final JLabel queuedCountLabel = new JLabel("0");
    private final JLabel completedCountLabel = new JLabel("0");
    private final JLabel cancelledCountLabel = new JLabel("0");
    private final JLabel busyGpusLabel = new JLabel("0 / 4");
    private final JLabel utilizationLabel = new JLabel("N/A");

    // 4 GPU cards
    private final JLabel[] gpuStatusLabels = new JLabel[4];
    private final JLabel[] gpuJobLabels = new JLabel[4];
    private final JLabel[] gpuRemainingLabels = new JLabel[4];
    private final JLabel[] gpuFinishLabels = new JLabel[4];

    // Dashboard tables
    private final DefaultTableModel waitingJobsModel;
    private final DefaultTableModel allowancesModel;
    private final DefaultTableModel completedJobsModel;

    public AdminDashboard(
            AdminBackend backend,
            PredictionService predictionService,
            UtilizationService utilizationService) {
        super("GPU Utilization Management \u00b7 Administrator");
        this.backend = backend;
        this.predictionService = predictionService;
        this.utilizationService = utilizationService;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1180, 760);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // --- Top Header Bar ---
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setBackground(Color.WHITE);
        headerBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 224, 230)),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JPanel titleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titleGroup.setOpaque(false);
        JLabel appTitle = new JLabel("GPU Utilization Dashboard");
        appTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        JLabel roleTag = new JLabel("Administrator");
        roleTag.setFont(new Font("SansSerif", Font.PLAIN, 13));
        roleTag.setForeground(new Color(100, 110, 120));
        titleGroup.add(appTitle);
        titleGroup.add(roleTag);
        headerBar.add(titleGroup, BorderLayout.WEST);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightControls.setOpaque(false);

        clockLabel = new JLabel("Simulated time: Not set");
        clockLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        clockLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 220)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));
        rightControls.add(clockLabel);

        advanceMinuteButton = new JButton("+1 minute");
        advanceMinuteButton.setBackground(new Color(24, 119, 242));
        advanceMinuteButton.setForeground(Color.WHITE);
        advanceMinuteButton.setFocusPainted(false);
        advanceMinuteButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        advanceMinuteButton.addActionListener(e -> handleAdvanceMinute());
        rightControls.add(advanceMinuteButton);

        JButton logoutButton = new JButton("Log out");
        logoutButton.setFocusPainted(false);
        logoutButton.addActionListener(e -> handleLogout());
        rightControls.add(logoutButton);

        headerBar.add(rightControls, BorderLayout.EAST);
        add(headerBar, BorderLayout.NORTH);

        // --- Dashboard Tab Content ---
        waitingJobsModel = createReadOnlyTableModel(
                new String[] { "Job", "Student", "Application", "Dur.", "Pos.", "Start", "Finish", "Wait" });
        allowancesModel = createReadOnlyTableModel(
                new String[] { "Student", "Name", "Alloc.", "Used", "Reserved", "Avail." });
        completedJobsModel = createReadOnlyTableModel(
                new String[] { "Job", "Student", "Application", "GPU", "Submitted", "Start", "Finish",
                        "Runtime / Queue wait", "Charged" });

        JPanel dashboardTab = buildDashboardTab();

        // --- Other 3 Tabs ---
        requestsPanel = new RequestsPanel(backend, this::refreshAll);
        studentsPanel = new StudentsPanel(backend, this::refreshAll);
        allJobsPanel = new AllJobsPanel(backend, predictionService, this::refreshAll);

        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Dashboard", new JScrollPane(dashboardTab));
        tabbedPane.addTab("Requests (0)", requestsPanel);
        tabbedPane.addTab("Students", studentsPanel);
        tabbedPane.addTab("All jobs", allJobsPanel);

        add(tabbedPane, BorderLayout.CENTER);

        // Initial load
        refreshAll();
    }

    private JPanel buildDashboardTab() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));
        container.setBackground(new Color(248, 249, 251));

        // 1. Summary KPI Row (6 tiles)
        JPanel kpiRow = new JPanel(new GridLayout(1, 6, 10, 0));
        kpiRow.setOpaque(false);
        kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));

        kpiRow.add(createKpiTile("Running", runningCountLabel, new Color(24, 119, 242)));
        kpiRow.add(createKpiTile("Queued", queuedCountLabel, new Color(180, 105, 14)));
        kpiRow.add(createKpiTile("Completed", completedCountLabel, new Color(20, 120, 70)));
        kpiRow.add(createKpiTile("Cancelled", cancelledCountLabel, new Color(110, 118, 129)));
        kpiRow.add(createKpiTile("Busy GPUs", busyGpusLabel, new Color(33, 37, 41)));
        kpiRow.add(createKpiTile("Utilization", utilizationLabel, new Color(24, 119, 242)));

        container.add(kpiRow);
        container.add(Box.createVerticalStrut(12));

        // 2. Four GPU Cards Row
        JPanel gpuRow = new JPanel(new GridLayout(1, 4, 10, 0));
        gpuRow.setOpaque(false);
        gpuRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 118));

        for (int i = 0; i < 4; i++) {
            gpuStatusLabels[i] = new JLabel("Free");
            gpuJobLabels[i] = new JLabel("-");
            gpuRemainingLabels[i] = new JLabel("-");
            gpuFinishLabels[i] = new JLabel("-");
            gpuRow.add(createGpuCard(i + 1, gpuStatusLabels[i], gpuJobLabels[i],
                    gpuRemainingLabels[i], gpuFinishLabels[i]));
        }

        container.add(gpuRow);
        container.add(Box.createVerticalStrut(14));

        // 3. Middle Split: Waiting Jobs (left) + Student Allowances (right)
        JPanel middleRow = new JPanel(new GridLayout(1, 2, 12, 0));
        middleRow.setOpaque(false);
        middleRow.setPreferredSize(new Dimension(1100, 210));
        middleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        middleRow.add(wrapTableCard("Waiting jobs", new JTable(waitingJobsModel)));
        middleRow.add(wrapTableCard("Student allowances", new JTable(allowancesModel)));

        container.add(middleRow);
        container.add(Box.createVerticalStrut(14));

        // 4. Bottom: Completed Jobs Table
        JPanel completedCard = wrapTableCard("Completed jobs", new JTable(completedJobsModel));
        completedCard.setPreferredSize(new Dimension(1100, 210));
        completedCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 260));
        container.add(completedCard);

        return container;
    }

    private JPanel createKpiTile(String label, JLabel valueLabel, Color accentColor) {
        JPanel tile = new JPanel(new BorderLayout(4, 4));
        tile.setBackground(Color.WHITE);
        tile.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 220, 226)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        JLabel titleLbl = new JLabel(label);
        titleLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titleLbl.setForeground(new Color(100, 110, 120));

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);

        tile.add(titleLbl, BorderLayout.NORTH);
        tile.add(valueLabel, BorderLayout.CENTER);
        return tile;
    }

    private JPanel createGpuCard(
            int gpuNumber,
            JLabel statusBadge,
            JLabel jobLbl,
            JLabel remLbl,
            JLabel finLbl) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 220, 226)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel nameLbl = new JLabel("GPU" + gpuNumber);
        nameLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        statusBadge.setFont(new Font("SansSerif", Font.BOLD, 12));
        top.add(nameLbl, BorderLayout.WEST);
        top.add(statusBadge, BorderLayout.EAST);

        JPanel details = new JPanel(new GridLayout(3, 2, 4, 3));
        details.setOpaque(false);
        details.add(new JLabel("Current job"));
        details.add(jobLbl);
        details.add(new JLabel("Remaining"));
        details.add(remLbl);
        details.add(new JLabel("Finishes"));
        details.add(finLbl);

        card.add(top, BorderLayout.NORTH);
        card.add(details, BorderLayout.CENTER);
        return card;
    }

    private JPanel wrapTableCard(String title, JTable table) {
        table.setRowHeight(24);
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 220, 226)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(new JScrollPane(table), BorderLayout.CENTER);
        return card;
    }

    private DefaultTableModel createReadOnlyTableModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /**
     * Refreshes EVERY tile, GPU card, table, and sub-tab across the Admin Dashboard.
     * Must be called after every submission, cancellation, completion, approval, grant, and +1 minute.
     */
    public void refreshAll() {
        SimulationClock clock = backend.getClock();
        LocalDateTime now = clock != null ? clock.getCurrentTime() : null;

        // 1. Update Clock & +1 Minute button
        if (now != null) {
            clockLabel.setText("Simulated time  " + now.format(FULL_FMT));
            advanceMinuteButton.setEnabled(true);
        } else {
            clockLabel.setText("Simulated time  Not set");
            advanceMinuteButton.setEnabled(false);
        }

        List<Job> allJobs = backend.getAllJobs();
        GPU[] gpus = backend.getGpus();

        // 2. Update Summary KPI tiles
        runningCountLabel.setText(String.valueOf(utilizationService.countJobsByStatus(allJobs, "RUNNING")));
        queuedCountLabel.setText(String.valueOf(utilizationService.countJobsByStatus(allJobs, "QUEUED")));
        completedCountLabel.setText(String.valueOf(utilizationService.countJobsByStatus(allJobs, "COMPLETED")));
        cancelledCountLabel.setText(String.valueOf(utilizationService.countJobsByStatus(allJobs, "CANCELLED")));

        int gpuCount = (gpus != null && gpus.length > 0) ? gpus.length : 4;
        busyGpusLabel.setText(utilizationService.countBusyGpus(gpus) + " / " + gpuCount);
        utilizationLabel.setText(utilizationService.formatUtilization(allJobs, clock, gpuCount));

        // 3. Update 4 GPU Cards
        for (int i = 0; i < 4; i++) {
            GPU gpu = (gpus != null && i < gpus.length) ? gpus[i] : null;
            if (gpu == null || gpu.isFree() || gpu.getCurrentJob() == null) {
                gpuStatusLabels[i].setText("Free");
                gpuStatusLabels[i].setForeground(new Color(100, 110, 120));
                gpuJobLabels[i].setText("-");
                gpuRemainingLabels[i].setText("-");
                gpuFinishLabels[i].setText("-");
            } else {
                Job job = gpu.getCurrentJob();
                gpuStatusLabels[i].setText("Busy");
                gpuStatusLabels[i].setForeground(new Color(20, 120, 70));
                gpuJobLabels[i].setText(job.getJobId());
                long rem = (now != null) ? job.getRemainingMinutes(now) : 0;
                gpuRemainingLabels[i].setText(rem + " min");
                LocalDateTime expFinish = job.getExpectedFinishTime();
                gpuFinishLabels[i].setText(expFinish != null ? expFinish.format(SHORT_TIME_FMT) : "-");
            }
        }

        // 4. Update Waiting Jobs Table (using PredictionService)
        waitingJobsModel.setRowCount(0);
        List<Job> waitingQueue = backend.getWaitingQueue();
        Map<String, PredictionService.JobPrediction> predictions =
                predictionService.computePredictions(gpus, waitingQueue, clock);

        if (waitingQueue != null) {
            for (Job job : waitingQueue) {
                if (job == null || !"QUEUED".equalsIgnoreCase(job.getStatus())) continue;
                PredictionService.JobPrediction pred = predictions.get(job.getJobId());
                waitingJobsModel.addRow(new Object[] {
                        job.getJobId(),
                        job.getStudentId(),
                        job.getApplicationName(),
                        job.getEstimatedDuration(),
                        pred != null ? pred.getQueuePosition() : "-",
                        pred != null ? pred.getPredictedStart().format(SHORT_TIME_FMT) : "-",
                        pred != null ? pred.getPredictedFinish().format(SHORT_TIME_FMT) : "-",
                        pred != null ? (pred.getRemainingWaitMinutes() + " min") : "-"
                });
            }
        }

        // 5. Update Student Allowances Table
        allowancesModel.setRowCount(0);
        List<Student> students = backend.getAllStudents();
        if (students != null) {
            for (Student s : students) {
                TimeAllowance a = s.getTimeAllowance();
                allowancesModel.addRow(new Object[] {
                        s.getUniId(),
                        s.getName(),
                        a != null ? a.getAllocated() : 0,
                        a != null ? a.getUsed() : 0,
                        a != null ? a.getReserved() : 0,
                        a != null ? a.getAvailable() : 0
                });
            }
        }

        // 6. Update Completed Jobs Table
        completedJobsModel.setRowCount(0);
        if (allJobs != null) {
            for (Job job : allJobs) {
                if (job != null && "COMPLETED".equalsIgnoreCase(job.getStatus())) {
                    String gpuStr = job.getAssignedGpuID() != null ? ("GPU" + job.getAssignedGpuID()) : "-";
                    String subStr = job.getSubmissionTime() != null ? job.getSubmissionTime().format(SHORT_TIME_FMT) : "-";
                    String startStr = job.getStartTime() != null ? job.getStartTime().format(SHORT_TIME_FMT) : "-";
                    String finStr = job.getFinishTime() != null ? job.getFinishTime().format(SHORT_TIME_FMT) : "-";
                    long qWait = (job.getSubmissionTime() != null && job.getStartTime() != null)
                            ? Duration.between(job.getSubmissionTime(), job.getStartTime()).toMinutes()
                            : 0;
                    String runtimeAndWait = job.getEstimatedDuration() + " min / " + qWait + " min";

                    completedJobsModel.addRow(new Object[] {
                            job.getJobId(),
                            job.getStudentId(),
                            job.getApplicationName(),
                            gpuStr,
                            subStr,
                            startStr,
                            finStr,
                            runtimeAndWait,
                            job.getEstimatedDuration()
                    });
                }
            }
        }

        // 7. Update Requests Tab Badge Count
        int pendingCount = 0;
        List<AccessRequest> requests = backend.getAccessRequests();
        if (requests != null) {
            for (AccessRequest req : requests) {
                if (req != null && "PENDING".equalsIgnoreCase(req.getStatus())) {
                    pendingCount++;
                }
            }
        }
        tabbedPane.setTitleAt(1, "Requests (" + pendingCount + ")");

        // 8. Refresh sub-tabs
        requestsPanel.refresh();
        studentsPanel.refresh();
        allJobsPanel.refresh();
    }

    private void handleAdvanceMinute() {
        try {
            backend.advanceMinute();
            refreshAll();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not advance clock: " + ex.getMessage(),
                    "Simulation Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleLogout() {
        dispose();
        if (backend != null) {
            backend.onLogout();
        }
    }
}
