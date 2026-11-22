package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import model.Student;
import model.Job;
import logic.Scheduler;

public class StudentPanel {

    private JPanel panel;
    private JTabbedPane tabs;
    private Student student;
    private Scheduler scheduler;
    private JTextField nameField;
    private JTextField appField;
    private JTextArea descField;
    private JComboBox<String> sizeBox;
    private JLabel allowLabel;
    private JTable jobsTable;
    private DefaultTableModel jobsModel;
    private JTextField searchField;
    private JComboBox<String> statusBox;

    public StudentPanel(Student student, Scheduler scheduler)
    {
        this.student = student;
        this.scheduler = scheduler;

        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        tabs = new JTabbedPane();
        tabs.addTab("Home", makeHome());
        tabs.addTab("My Jobs", makeJobs());
        panel.add(tabs, BorderLayout.CENTER);
        refreshAllowance();
        refreshJobs();
    }

    public JPanel getPanel()
    {
        return panel;
    }

    private JPanel makeHome()
    {
        JPanel home = new JPanel();
        home.setLayout(new BorderLayout());

        JPanel top = new JPanel();
        top.setLayout(new GridLayout(3, 1));

        top.add(new JLabel("Welcome, " + student.getName()));

        top.add(new JLabel("University ID: " + student.getUniId()));

        allowLabel = new JLabel("");

        top.add(allowLabel);

        home.add(top, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setLayout(new GridLayout(5, 2, 10, 10));

        form.add(new JLabel("Job Title:"));
        nameField = new JTextField();
        form.add(nameField);

        form.add(new JLabel("Application Name:"));
        appField = new JTextField();
        form.add(appField);

        form.add(new JLabel("Description:"));
        descField = new JTextArea();
        form.add(new JScrollPane(descField));

        form.add(new JLabel("Task Size:"));

        String[] sizes = {"Small", "Medium", "Large", "Very Large"};

        sizeBox = new JComboBox<String>(sizes);
        form.add(sizeBox);

        JButton submitBtn = new JButton("Submit Job");
        form.add(submitBtn);

        home.add(form, BorderLayout.CENTER);

        submitBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                submitJob();
            }
        });

        return home;
    }

    private JPanel makeJobs()
    {
        JPanel jobs = new JPanel();
        jobs.setLayout(new BorderLayout());

        JPanel top = new JPanel();

        top.add(new JLabel("Search:"));

        searchField = new JTextField(12);
        top.add(searchField);

        top.add(new JLabel("Status:"));

        String[] statuses = {"All", "QUEUED", "RUNNING", "COMPLETED", "CANCELLED"};

        statusBox = new JComboBox<String>(statuses);
        top.add(statusBox);

        JButton searchBtn = new JButton("Search");
        top.add(searchBtn);

        jobs.add(top, BorderLayout.NORTH);

        String[] cols = {"Job ID", "Application", "Status", "GPU", "Duration", "Wait", "Result"};

        jobsModel = new DefaultTableModel(cols, 0);

        jobsTable = new JTable(jobsModel);

        jobs.add(new JScrollPane(jobsTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel();

        JButton refreshBtn = new JButton("Refresh");
        JButton cancelBtn = new JButton("Cancel Job");

        bottom.add(refreshBtn);
        bottom.add(cancelBtn);

        jobs.add(bottom, BorderLayout.SOUTH);

        searchBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                refreshJobs();
            }
        });

        refreshBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                refreshJobs();
                refreshAllowance();
            }
        });

        cancelBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                cancelJob();
            }
        });

        return jobs;
    }

    private void refreshAllowance()
    {
        if (student.getTimeAllowance() == null)
        {
            allowLabel.setText("GPU Allowance: Not available");
            return;
        }

        allowLabel.setText("Allocated: " + student.getTimeAllowance().getAllocated() + " | Used: " + student.getTimeAllowance().getUsed() + " | Reserved: " + student.getTimeAllowance().getReserved() + " | Available: " + student.getTimeAllowance().getAvailable());
    }

    private void submitJob()
    {
        String title = nameField.getText();
        String app = appField.getText();
        String desc = descField.getText();
        String size = (String) sizeBox.getSelectedItem();

        if (title.trim().isEmpty() || app.trim().isEmpty() || desc.trim().isEmpty())
        {
            JOptionPane.showMessageDialog(panel, "Please fill all job fields.");
            return;
        }

        int mins = 0;

        if (size.equals("Small"))
        {
            mins = 5;
        }
        else if (size.equals("Medium"))
        {
            mins = 15;
        }
        else if (size.equals("Large"))
        {
            mins = 30;
        }
        else if (size.equals("Very Large"))
        {
            mins = 60;
        }

        int confirm = JOptionPane.showConfirmDialog(panel, "This job will take " + mins + " GPU minutes. Submit?", "Confirm Job", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION)
        {
            try
            {
                Job job = scheduler.submitJob(student.getUniId(), title, app, desc, size);

                JOptionPane.showMessageDialog(panel, "Job submitted successfully. Job ID: " + job.getJobId());

                nameField.setText("");
                appField.setText("");
                descField.setText("");

                refreshJobs();
                refreshAllowance();
            }
            catch (IllegalArgumentException e)
            {
                JOptionPane.showMessageDialog(panel, e.getMessage());
            }
            catch (IllegalStateException e)
            {
                JOptionPane.showMessageDialog(panel, e.getMessage());
            }
        }
    }

    private void refreshJobs()
    {
        jobsModel.setRowCount(0);

        ArrayList<Job> jobs = scheduler.getJobs();

        String search = searchField.getText().trim();
        String status = (String) statusBox.getSelectedItem();

        for (int i = 0; i < jobs.size(); i++)
        {
            Job job = jobs.get(i);

            if (job.getStudentId().equals(student.getUniId()))
            {
                boolean found = false;

                if (search.isEmpty())
                {
                    found = true;
                }
                else if (job.getJobId().equalsIgnoreCase(search) || job.getTitle().equalsIgnoreCase(search))
                {
                    found = true;
                }

                if (found && (status.equals("All") || job.getStatus().equals(status)))
                {
                    String gpu = "None";

                    if (job.getAssignedGpuID() != null)
                    {
                        gpu = "GPU " + job.getAssignedGpuID();
                    }

                    String wait = "-";

                    if (job.getStatus().equals("QUEUED"))
                    {
                        wait = "Waiting";
                    }
                    else if (job.getStatus().equals("RUNNING"))
                    {
                        wait = "0";
                    }

                    String result = "-";

                    if (job.getStatus().equals("COMPLETED"))
                    {
                        result = "Completed";
                    }
                    else if (job.getStatus().equals("CANCELLED"))
                    {
                        result = "Cancelled";
                    }

                    Object[] row = {job.getJobId(), job.getApplicationName(), job.getStatus(), gpu, job.getEstimatedDuration(), wait, result};

                    jobsModel.addRow(row);
                }
            }
        }
    }

    private void cancelJob()
    {
        int row = jobsTable.getSelectedRow();

        if (row == -1)
        {
            JOptionPane.showMessageDialog(panel, "Select a job first.");
            return;
        }

        String jobId = jobsModel.getValueAt(jobsTable.convertRowIndexToModel(row), 0).toString();

        String reason = JOptionPane.showInputDialog(panel, "Enter cancellation reason:");

        if (reason == null)
        {
            return;
        }

        if (reason.trim().isEmpty())
        {
            JOptionPane.showMessageDialog(panel, "Cancellation reason is required.");
            return;
        }

        try
        {
            scheduler.cancelJob(jobId, student, reason);

            JOptionPane.showMessageDialog(panel, "Job cancelled successfully.");
            refreshJobs();
            refreshAllowance();
        }
        catch (IllegalArgumentException e)
        {
            JOptionPane.showMessageDialog(panel, e.getMessage());
        }
        catch (IllegalStateException e)
        {
            JOptionPane.showMessageDialog(panel, e.getMessage());
        }
    }

}