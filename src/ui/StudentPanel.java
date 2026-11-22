package ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import model.Student;

public class StudentPanel {

    private JPanel panel;
    private JTabbedPane tabs;
    private Student student;
    private JTextField nameField;
    private JTextField appField;
    private JTextArea descField;
    private JComboBox<String> sizeBox;
    private JLabel allowLabel;
    private JTable jobsTable;
    private DefaultTableModel jobsModel;

    public StudentPanel(Student student)
    {
        this.student = student;

        panel = new JPanel();
        panel.setLayout(new BorderLayout());

        tabs = new JTabbedPane();

        tabs.addTab("Home", makeHome());
        tabs.addTab("My Jobs", makeJobs());

        panel.add(tabs, BorderLayout.CENTER);
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

        allowLabel = new JLabel("GPU Allowance: Not available yet");

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

        refreshBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                refreshJobs();
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

    private void submitJob()
    {
        String title = nameField.getText();
        String app = appField.getText();
        String desc = descField.getText();

        String size = (String) sizeBox.getSelectedItem();

        if (title.trim().isEmpty() ||
            app.trim().isEmpty() ||
            desc.trim().isEmpty())
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

        int confirm = JOptionPane.showConfirmDialog(
            panel,
            "This job will take " + mins + " GPU minutes. Submit?",
            "Confirm Job",
            JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION)
        {
            // Connect to Member 2's Scheduler here.

            JOptionPane.showMessageDialog(
                panel,
                "Job submission is not connected yet."
            );
        }
    }

    private void refreshJobs()
    {
        jobsModel.setRowCount(0);

        // Get this student's jobs from Member 2's Scheduler.
        // Add the jobs to the table here.
    }

    private void cancelJob()
    {
        int row = jobsTable.getSelectedRow();

        if (row == -1)
        {
            JOptionPane.showMessageDialog(panel, "Select a job first.");
            return;
        }

        String reason = JOptionPane.showInputDialog(
            panel,
            "Enter cancellation reason:"
        );

        if (reason == null)
        {
            return;
        }

        if (reason.trim().isEmpty())
        {
            JOptionPane.showMessageDialog(
                panel,
                "Cancellation reason is required."
            );
            return;
        }

        // Connect to Member 2's Scheduler cancellation method.

        JOptionPane.showMessageDialog(
            panel,
            "Job cancellation is not connected yet."
        );
    }

}