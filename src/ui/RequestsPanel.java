package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

import model.AccessRequest;
import model.Student;
import model.TimeAllowance;

public class RequestsPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final AdminBackend backend;
    private final Runnable onDataChanged;

    private final JComboBox<String> statusFilterCombo;
    private final DefaultTableModel tableModel;
    private final JTable requestsTable;
    private final JLabel selectedInfoLabel;
    private final JSpinner initialMinutesSpinner;
    private final JButton approveButton;
    private final JButton rejectButton;

    // Keeps track of the currently displayed requests so table row index maps to AccessRequest.
    private final List<AccessRequest> displayedRequests = new ArrayList<>();

    public RequestsPanel(AdminBackend backend, Runnable onDataChanged) {
        this.backend = backend;
        this.onDataChanged = onDataChanged;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // --- Top Bar: Title + Filter ---
        JPanel topBar = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Registration Requests");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        topBar.add(titleLabel, BorderLayout.WEST);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterPanel.add(new JLabel("Show:"));
        statusFilterCombo = new JComboBox<>(new String[] { "Pending", "Approved", "Rejected", "All" });
        statusFilterCombo.addActionListener(e -> refresh());
        filterPanel.add(statusFilterCombo);
        topBar.add(filterPanel, BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);

        // --- Center: Requests Table ---
        String[] columns = { "University ID", "Name", "Email", "Phone", "Password", "Status" };
        tableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        requestsTable = new JTable(tableModel);
        requestsTable.setRowHeight(26);
        requestsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        requestsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateActionBar();
            }
        });

        add(new JScrollPane(requestsTable), BorderLayout.CENTER);

        // --- Bottom: Selected Request Controls ---
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 8));
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 215, 220)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        selectedInfoLabel = new JLabel("Select a pending request to review.");
        selectedInfoLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        bottomPanel.add(selectedInfoLabel, BorderLayout.NORTH);

        JPanel controlsRow = new JPanel(new BorderLayout());

        JPanel minutesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        minutesPanel.add(new JLabel("Initial GPU minutes:"));
        initialMinutesSpinner = new JSpinner(new SpinnerNumberModel(TimeAllowance.DEFAULT_MINUTES, 1, 10000, 10));
        minutesPanel.add(initialMinutesSpinner);
        controlsRow.add(minutesPanel, BorderLayout.WEST);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        approveButton = new JButton("Approve");
        approveButton.setBackground(new Color(24, 119, 242));
        approveButton.setForeground(Color.WHITE);
        approveButton.setFocusPainted(false);
        approveButton.addActionListener(e -> handleApprove());

        rejectButton = new JButton("Reject");
        rejectButton.setBackground(new Color(217, 72, 15));
        rejectButton.setForeground(Color.WHITE);
        rejectButton.setFocusPainted(false);
        rejectButton.addActionListener(e -> handleReject());

        buttonsPanel.add(approveButton);
        buttonsPanel.add(rejectButton);
        controlsRow.add(buttonsPanel, BorderLayout.EAST);

        bottomPanel.add(controlsRow, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        refresh();
    }

    /**
     * Reloads the table from the backend according to the selected status filter.
     */
    public void refresh() {
        tableModel.setRowCount(0);
        displayedRequests.clear();

        String filter = (String) statusFilterCombo.getSelectedItem();
        List<AccessRequest> allRequests = backend.getAccessRequests();

        if (allRequests != null) {
            for (AccessRequest req : allRequests) {
                if (req == null || req.getStudent() == null) {
                    continue;
                }
                String status = req.getStatus() != null ? req.getStatus() : "";
                if ("All".equalsIgnoreCase(filter) || status.equalsIgnoreCase(filter)) {
                    displayedRequests.add(req);
                    Student s = req.getStudent();
                    tableModel.addRow(new Object[] {
                            s.getUniId(),
                            s.getName(),
                            s.getEmail(),
                            s.getPhoneNum(),
                            "••••••",
                            formatStatus(status)
                    });
                }
            }
        }

        updateActionBar();
    }

    /**
     * Enables Approve/Reject buttons ONLY when a PENDING request is selected.
     */
    private void updateActionBar() {
        int row = requestsTable.getSelectedRow();
        if (row < 0 || row >= displayedRequests.size()) {
            selectedInfoLabel.setText("Select a pending request to review.");
            approveButton.setEnabled(false);
            rejectButton.setEnabled(false);
            initialMinutesSpinner.setEnabled(false);
            return;
        }

        AccessRequest req = displayedRequests.get(row);
        Student s = req.getStudent();
        boolean isPending = "PENDING".equalsIgnoreCase(req.getStatus());

        selectedInfoLabel.setText("Selected: " + s.getName() + " (" + s.getUniId() + ") — Status: "
                + formatStatus(req.getStatus()));
        approveButton.setEnabled(isPending);
        rejectButton.setEnabled(isPending);
        initialMinutesSpinner.setEnabled(isPending);
    }

    private void handleApprove() {
        int row = requestsTable.getSelectedRow();
        if (row < 0 || row >= displayedRequests.size()) {
            return;
        }
        AccessRequest req = displayedRequests.get(row);
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            JOptionPane.showMessageDialog(this,
                    "This request has already been processed.",
                    "Already Processed",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int initialMinutes = (Integer) initialMinutesSpinner.getValue();
        if (initialMinutes <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Initial GPU minutes must be a positive whole number.",
                    "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            backend.approveRequest(req, initialMinutes);
            JOptionPane.showMessageDialog(this,
                    "Approved " + req.getStudent().getName() + " (" + req.getStudent().getUniId()
                            + ") with " + initialMinutes + " GPU minutes.",
                    "Request Approved",
                    JOptionPane.INFORMATION_MESSAGE);
            if (onDataChanged != null) {
                onDataChanged.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not approve request: " + ex.getMessage(),
                    "Approval Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleReject() {
        int row = requestsTable.getSelectedRow();
        if (row < 0 || row >= displayedRequests.size()) {
            return;
        }
        AccessRequest req = displayedRequests.get(row);
        if (!"PENDING".equalsIgnoreCase(req.getStatus())) {
            JOptionPane.showMessageDialog(this,
                    "This request has already been processed.",
                    "Already Processed",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int choice = JOptionPane.showConfirmDialog(this,
                "Reject registration request for " + req.getStudent().getName()
                        + " (" + req.getStudent().getUniId() + ")?",
                "Confirm Rejection",
                JOptionPane.YES_NO_OPTION);

        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            backend.rejectRequest(req);
            JOptionPane.showMessageDialog(this,
                    "Rejected request for " + req.getStudent().getName() + ".",
                    "Request Rejected",
                    JOptionPane.INFORMATION_MESSAGE);
            if (onDataChanged != null) {
                onDataChanged.run();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not reject request: " + ex.getMessage(),
                    "Rejection Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatStatus(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return raw.substring(0, 1).toUpperCase() + raw.substring(1).toLowerCase();
    }
}
