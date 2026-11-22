package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

import logic.PredictionService;
import logic.SimulationClock;
import model.Job;

public class AllJobsPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AdminBackend backend;
    private final PredictionService predictionService;
    private final Runnable onDataChanged;

    private final JTextField searchField;
    private final JComboBox<String> statusFilterCombo;
    private final JButton cancelButton;
    private final DefaultTableModel tableModel;
    private final JTable jobsTable;

    private final List<Job> displayedJobs = new ArrayList<>();

    public AllJobsPanel(AdminBackend backend, PredictionService predictionService, Runnable onDataChanged) {
        this.backend = backend;
        this.predictionService = predictionService;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // --- Top Bar: Search + Status Filter + Cancel Button ---
        JPanel topBar = new JPanel(new BorderLayout(10, 0));

        JPanel filterGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterGroup.add(new JLabel("Search:"));
        searchField = new JTextField(22);
        searchField.setToolTipText("Search by Job ID, Student ID, Title, or Application");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { refresh(); }
            @Override
            public void removeUpdate(DocumentEvent e) { refresh(); }
            @Override
            public void changedUpdate(DocumentEvent e) { refresh(); }
        });
        filterGroup.add(searchField);

        statusFilterCombo = new JComboBox<>(new String[] {
                "All statuses", "Queued", "Running", "Completed", "Cancelled"
        });
        statusFilterCombo.addActionListener(e -> refresh());
        filterGroup.add(statusFilterCombo);

        topBar.add(filterGroup, BorderLayout.WEST);

        cancelButton = new JButton("Cancel selected job");
        cancelButton.setBackground(new Color(217, 72, 15));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setFocusPainted(false);
        cancelButton.setEnabled(false);
        cancelButton.addActionListener(e -> handleCancelSelected());
        topBar.add(cancelButton, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // --- Center: All Jobs Table ---
        String[] columns = {
                "Job", "Student", "Title", "Application", "Size", "Status",
                "GPU", "Submitted", "Start", "Finish", "Wait / Rem.", "Charged", "Cancel Info"
        };
        tableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        jobsTable = new JTable(tableModel);
        jobsTable.setRowHeight(25);
        jobsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        jobsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jobsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateCancelButtonState();
            }
        });

        // Set comfortable column widths
        int[] widths = { 60, 95, 130, 100, 95, 90, 60, 120, 125, 125, 85, 65, 180 };
        for (int i = 0; i < widths.length; i++) {
            jobsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        add(new JScrollPane(jobsTable), BorderLayout.CENTER);

        // --- Footer Legend ---
        JLabel legendLabel = new JLabel("Note: \u2248 indicates predicted start/finish for queued jobs. Cancelled jobs remain in history.");
        legendLabel.setForeground(new Color(100, 110, 120));
        add(legendLabel, BorderLayout.SOUTH);

        refresh();
    }

    /**
     * Rebuilds the All Jobs table using current backend jobs and fresh predictions.
     */
    public void refresh() {
        tableModel.setRowCount(0);
        displayedJobs.clear();

        List<Job> allJobs = backend.getAllJobs();
        if (allJobs == null) {
            updateCancelButtonState();
            return;
        }

        SimulationClock clock = backend.getClock();
        LocalDateTime now = clock != null ? clock.getCurrentTime() : null;
        Map<String, PredictionService.JobPrediction> predictions =
                predictionService.computePredictions(backend.getGpus(), backend.getWaitingQueue(), clock);

        String query = searchField.getText().trim().toLowerCase();
        String statusFilter = (String) statusFilterCombo.getSelectedItem();

        for (Job job : allJobs) {
            if (job == null) continue;

            String status = job.getStatus() != null ? job.getStatus() : "";
            if (!"All statuses".equalsIgnoreCase(statusFilter) && !status.equalsIgnoreCase(statusFilter)) {
                continue;
            }

            if (!query.isEmpty() && !matchesQuery(job, query)) {
                continue;
            }

            displayedJobs.add(job);
            PredictionService.JobPrediction pred = predictions.get(job.getJobId());

            String statusDisplay = formatStatus(status);
            if ("QUEUED".equalsIgnoreCase(status) && pred != null) {
                statusDisplay = "Queued #" + pred.getQueuePosition();
            }

            String gpuStr = job.getAssignedGpuID() != null ? ("GPU" + job.getAssignedGpuID()) : "-";
            String submittedStr = job.getSubmissionTime() != null ? job.getSubmissionTime().format(TIME_FMT) : "-";

            String startStr = "-";
            String finishStr = "-";
            String waitOrRemStr = "-";
            String chargedStr = "-";
            String cancelInfoStr = "-";

            if ("QUEUED".equalsIgnoreCase(status)) {
                if (pred != null) {
                    startStr = "\u2248 " + pred.getPredictedStart().format(TIME_FMT);
                    finishStr = "\u2248 " + pred.getPredictedFinish().format(TIME_FMT);
                    waitOrRemStr = pred.getRemainingWaitMinutes() + " min wait";
                }
            } else if ("RUNNING".equalsIgnoreCase(status)) {
                startStr = job.getStartTime() != null ? job.getStartTime().format(TIME_FMT) : "-";
                finishStr = job.getExpectedFinishTime() != null ? job.getExpectedFinishTime().format(TIME_FMT) : "-";
                long rem = (now != null) ? job.getRemainingMinutes(now) : 0;
                waitOrRemStr = rem + " min left";
            } else if ("COMPLETED".equalsIgnoreCase(status)) {
                startStr = job.getStartTime() != null ? job.getStartTime().format(TIME_FMT) : "-";
                finishStr = job.getFinishTime() != null ? job.getFinishTime().format(TIME_FMT) : "-";
                if (job.getSubmissionTime() != null && job.getStartTime() != null) {
                    long qWait = Duration.between(job.getSubmissionTime(), job.getStartTime()).toMinutes();
                    waitOrRemStr = qWait + " min wait";
                }
                chargedStr = String.valueOf(job.getEstimatedDuration());
            } else if ("CANCELLED".equalsIgnoreCase(status)) {
                startStr = job.getStartTime() != null ? job.getStartTime().format(TIME_FMT) : "-";
                finishStr = job.getFinishTime() != null ? job.getFinishTime().format(TIME_FMT) : "-";
                long charged = (job.getStartTime() == null) ? 0
                        : job.getElapsedMinutes(now != null ? now : job.getFinishTime());
                chargedStr = String.valueOf(charged);
                String by = job.getCancelledBy() != null ? job.getCancelledBy() : "?";
                String reason = job.getCancellationReason() != null ? job.getCancellationReason() : "";
                String when = job.getCancellationTime() != null ? job.getCancellationTime().format(TIME_FMT) : "";
                cancelInfoStr = by + " @ " + when + ": " + reason;
            }

            tableModel.addRow(new Object[] {
                    job.getJobId(),
                    job.getStudentId(),
                    job.getTitle(),
                    job.getApplicationName(),
                    job.getTaskSize() + " (" + job.getEstimatedDuration() + ")",
                    statusDisplay,
                    gpuStr,
                    submittedStr,
                    startStr,
                    finishStr,
                    waitOrRemStr,
                    chargedStr,
                    cancelInfoStr
            });
        }

        updateCancelButtonState();
    }

    private boolean matchesQuery(Job job, String query) {
        return contains(job.getJobId(), query)
                || contains(job.getStudentId(), query)
                || contains(job.getTitle(), query)
                || contains(job.getApplicationName(), query);
    }

    private boolean contains(String field, String query) {
        return field != null && field.toLowerCase().contains(query);
    }

    private void updateCancelButtonState() {
        int row = jobsTable.getSelectedRow();
        if (row < 0 || row >= displayedJobs.size()) {
            cancelButton.setEnabled(false);
            return;
        }
        String status = displayedJobs.get(row).getStatus();
        cancelButton.setEnabled("QUEUED".equalsIgnoreCase(status) || "RUNNING".equalsIgnoreCase(status));
    }

    private void handleCancelSelected() {
        int row = jobsTable.getSelectedRow();
        if (row < 0 || row >= displayedJobs.size()) {
            return;
        }
        Job job = displayedJobs.get(row);
        String status = job.getStatus();

        if (!"QUEUED".equalsIgnoreCase(status) && !"RUNNING".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this,
                    "Only Queued or Running jobs can be cancelled.",
                    "Cannot Cancel",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        String chargeSummary;
        if ("QUEUED".equalsIgnoreCase(status)) {
            chargeSummary = job.getJobId() + " is currently Queued.\n"
                    + "0 min will be charged; all " + job.getEstimatedDuration()
                    + " reserved min will return to the student's balance.";
        } else {
            SimulationClock clock = backend.getClock();
            long elapsed = (clock != null) ? job.getElapsedMinutes(clock.getCurrentTime()) : 0;
            long returned = Math.max(0, job.getEstimatedDuration() - elapsed);
            chargeSummary = job.getJobId() + " is running on GPU" + job.getAssignedGpuID()
                    + " and has run " + elapsed + " min.\n"
                    + elapsed + " min will be charged; the other " + returned
                    + " min return to the balance.";
        }

        String reason = JOptionPane.showInputDialog(this,
                chargeSummary + "\n\nEnter cancellation reason (required):",
                "Cancel job " + job.getJobId(),
                JOptionPane.WARNING_MESSAGE);

        if (reason == null) {
            return;
        }
        if (reason.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "A non-blank reason is required to cancel a job.",
                    "Reason Required",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            backend.cancelJob(job.getJobId(), "admin", reason.trim());
            if (onDataChanged != null) {
                onDataChanged.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not cancel job: " + ex.getMessage(),
                    "Cancellation Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatStatus(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
    }
}
