package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import logic.SimulationClock;
import model.Job;
import model.Student;
import model.TimeAllowance;

public class StudentsPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AdminBackend backend;
    private final Runnable onDataChanged;

    private final JTextField searchIdField;
    private final JLabel studentDetailsLabel;

    private final JLabel allocatedValueLabel;
    private final JLabel usedValueLabel;
    private final JLabel reservedValueLabel;
    private final JLabel availableValueLabel;

    private final JTextField grantMinutesField;
    private final JTextField grantReasonField;
    private final JButton grantButton;

    private final JLabel jobsHeaderLabel;
    private final JButton cancelJobButton;
    private final DefaultTableModel jobsTableModel;
    private final JTable jobsTable;

    private Student currentStudent;
    private final List<Job> displayedJobs = new ArrayList<>();

    public StudentsPanel(AdminBackend backend, Runnable onDataChanged) {
        this.backend = backend;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // --- Top Section: Search Bar + Student Info + Balance Tiles + Grant Extra Time ---
        JPanel topContainer = new JPanel(new BorderLayout(10, 10));

        // 1. Search bar
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchPanel.add(new JLabel("University ID:"));
        searchIdField = new JTextField(18);
        searchIdField.addActionListener(e -> handleSearch());
        searchPanel.add(searchIdField);

        JButton searchButton = new JButton("Search");
        searchButton.setBackground(new Color(24, 119, 242));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> handleSearch());
        searchPanel.add(searchButton);

        studentDetailsLabel = new JLabel("Enter a student University ID and click Search.");
        studentDetailsLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        studentDetailsLabel.setForeground(new Color(70, 80, 90));
        searchPanel.add(studentDetailsLabel);

        topContainer.add(searchPanel, BorderLayout.NORTH);

        // 2. Balance Cards (Allocated, Used, Reserved, Available)
        JPanel balanceRow = new JPanel(new GridLayout(1, 4, 10, 0));
        allocatedValueLabel = new JLabel("-");
        usedValueLabel = new JLabel("-");
        reservedValueLabel = new JLabel("-");
        availableValueLabel = new JLabel("-");

        balanceRow.add(createStatCard("Allocated", allocatedValueLabel, new Color(33, 37, 41)));
        balanceRow.add(createStatCard("Used", usedValueLabel, new Color(33, 37, 41)));
        balanceRow.add(createStatCard("Reserved", reservedValueLabel, new Color(180, 105, 14)));
        balanceRow.add(createStatCard("Available", availableValueLabel, new Color(20, 120, 70)));

        topContainer.add(balanceRow, BorderLayout.CENTER);

        // 3. Grant Extra Time Panel
        JPanel grantPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        grantPanel.setBorder(BorderFactory.createTitledBorder("Grant Extra Time"));
        grantPanel.add(new JLabel("Minutes:"));
        grantMinutesField = new JTextField(6);
        grantPanel.add(grantMinutesField);

        grantPanel.add(new JLabel("Reason (required):"));
        grantReasonField = new JTextField(26);
        grantPanel.add(grantReasonField);

        grantButton = new JButton("Grant");
        grantButton.setBackground(new Color(24, 119, 242));
        grantButton.setForeground(Color.WHITE);
        grantButton.setFocusPainted(false);
        grantButton.setEnabled(false);
        grantButton.addActionListener(e -> handleGrantTime());
        grantPanel.add(grantButton);

        topContainer.add(grantPanel, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // --- Center Section: Jobs of Selected Student ---
        JPanel jobsSection = new JPanel(new BorderLayout(8, 8));

        JPanel jobsHeaderBar = new JPanel(new BorderLayout());
        jobsHeaderLabel = new JLabel("Student Jobs");
        jobsHeaderLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        jobsHeaderBar.add(jobsHeaderLabel, BorderLayout.WEST);

        cancelJobButton = new JButton("Cancel job...");
        cancelJobButton.setBackground(new Color(217, 72, 15));
        cancelJobButton.setForeground(Color.WHITE);
        cancelJobButton.setFocusPainted(false);
        cancelJobButton.setEnabled(false);
        cancelJobButton.addActionListener(e -> handleCancelSelectedJob());
        jobsHeaderBar.add(cancelJobButton, BorderLayout.EAST);

        jobsSection.add(jobsHeaderBar, BorderLayout.NORTH);

        String[] columns = { "Job", "Title", "Application", "Size", "Status", "GPU", "Start", "Charged" };
        jobsTableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        jobsTable = new JTable(jobsTableModel);
        jobsTable.setRowHeight(25);
        jobsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jobsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateCancelButtonState();
            }
        });

        jobsSection.add(new JScrollPane(jobsTable), BorderLayout.CENTER);
        add(jobsSection, BorderLayout.CENTER);
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color valueColor) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 220)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titleLbl.setForeground(new Color(100, 110, 120));

        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        valueLabel.setForeground(valueColor);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void handleSearch() {
        String uniId = searchIdField.getText().trim();
        if (uniId.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter a student University ID.",
                    "Missing University ID",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        Student found = backend.findStudentById(uniId);
        if (found == null) {
            currentStudent = null;
            clearStudentView();
            JOptionPane.showMessageDialog(this,
                    "No approved student found with University ID: " + uniId,
                    "Student Not Found",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        currentStudent = found;
        refresh();
    }

    /**
     * Re-fetches the currently selected student's balance and jobs from the backend.
     */
    public void refresh() {
        if (currentStudent == null) {
            return;
        }
        // Re-fetch in case object reference changed after reload
        Student refreshed = backend.findStudentById(currentStudent.getUniId());
        if (refreshed == null) {
            currentStudent = null;
            clearStudentView();
            return;
        }
        currentStudent = refreshed;

        studentDetailsLabel.setText("  " + currentStudent.getName() + " | "
                + currentStudent.getEmail() + " | " + currentStudent.getPhoneNum());

        TimeAllowance allowance = currentStudent.getTimeAllowance();
        if (allowance != null) {
            allocatedValueLabel.setText(String.valueOf(allowance.getAllocated()));
            usedValueLabel.setText(String.valueOf(allowance.getUsed()));
            reservedValueLabel.setText(String.valueOf(allowance.getReserved()));
            availableValueLabel.setText(String.valueOf(allowance.getAvailable()));
        } else {
            allocatedValueLabel.setText("-");
            usedValueLabel.setText("-");
            reservedValueLabel.setText("-");
            availableValueLabel.setText("-");
        }

        grantButton.setEnabled(true);
        jobsHeaderLabel.setText("Jobs of " + currentStudent.getName() + " (" + currentStudent.getUniId() + ")");

        jobsTableModel.setRowCount(0);
        displayedJobs.clear();

        List<Job> studentJobs = backend.getJobsForStudent(currentStudent.getUniId());
        SimulationClock clock = backend.getClock();
        LocalDateTime now = clock != null ? clock.getCurrentTime() : null;

        if (studentJobs != null) {
            for (Job job : studentJobs) {
                displayedJobs.add(job);
                String gpuStr = job.getAssignedGpuID() != null ? ("GPU" + job.getAssignedGpuID()) : "-";
                String startStr = job.getStartTime() != null ? job.getStartTime().format(TIME_FMT) : "-";
                String chargedStr = formatChargedMinutes(job, now);

                jobsTableModel.addRow(new Object[] {
                        job.getJobId(),
                        job.getTitle(),
                        job.getApplicationName(),
                        job.getTaskSize() + " (" + job.getEstimatedDuration() + ")",
                        formatStatus(job.getStatus()),
                        gpuStr,
                        startStr,
                        chargedStr
                });
            }
        }

        updateCancelButtonState();
    }

    private void clearStudentView() {
        studentDetailsLabel.setText("Enter a student University ID and click Search.");
        allocatedValueLabel.setText("-");
        usedValueLabel.setText("-");
        reservedValueLabel.setText("-");
        availableValueLabel.setText("-");
        grantButton.setEnabled(false);
        cancelJobButton.setEnabled(false);
        jobsHeaderLabel.setText("Student Jobs");
        jobsTableModel.setRowCount(0);
        displayedJobs.clear();
    }

    private void updateCancelButtonState() {
        int row = jobsTable.getSelectedRow();
        if (row < 0 || row >= displayedJobs.size()) {
            cancelJobButton.setEnabled(false);
            return;
        }
        String status = displayedJobs.get(row).getStatus();
        boolean cancellable = "QUEUED".equalsIgnoreCase(status) || "RUNNING".equalsIgnoreCase(status);
        cancelJobButton.setEnabled(cancellable);
    }

    private void handleGrantTime() {
        if (currentStudent == null) {
            return;
        }
        String minutesText = grantMinutesField.getText().trim();
        String reason = grantReasonField.getText().trim();

        int minutes;
        try {
            minutes = Integer.parseInt(minutesText);
            if (minutes <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Minutes must be a positive whole number (e.g., 20).",
                    "Invalid Minutes",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (reason.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "A reason is required when granting extra GPU time.",
                    "Reason Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            backend.grantExtraTime(currentStudent, minutes, reason);
            grantMinutesField.setText("");
            grantReasonField.setText("");
            JOptionPane.showMessageDialog(this,
                    "Granted +" + minutes + " GPU minutes to " + currentStudent.getName() + ".",
                    "Time Granted",
                    JOptionPane.INFORMATION_MESSAGE);
            if (onDataChanged != null) {
                onDataChanged.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not grant extra time: " + ex.getMessage(),
                    "Grant Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleCancelSelectedJob() {
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
            return; // User clicked Cancel in the dialog
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

    private String formatChargedMinutes(Job job, LocalDateTime now) {
        String status = job.getStatus();
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return String.valueOf(job.getEstimatedDuration());
        }
        if ("CANCELLED".equalsIgnoreCase(status)) {
            if (job.getStartTime() == null) {
                return "0";
            }
            return String.valueOf(job.getElapsedMinutes(now != null ? now : job.getFinishTime()));
        }
        return "-";
    }

    private String formatStatus(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
    }
}
