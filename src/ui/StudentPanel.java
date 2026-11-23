package ui;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import model.Student;
import model.Job;
import logic.Scheduler;
import logic.PredictionService;
import logic.ValidationService;

public class StudentPanel {
    private JPanel panel;
    private JTabbedPane tabs;
    private Student student;
    private Scheduler scheduler;
    private LoginFrame loginFrame;
    private PredictionService predictionService;
    private JTextField nameField;
    private JTextField appField;
    private JTextArea descField;
    private JComboBox<String> sizeBox;
    private JLabel allowLabel;
    private JLabel minsLabel;
    private JLabel clockLabel;
    private JTable jobsTable;
    private DefaultTableModel jobsModel;
    private JTextField searchField;
    private JComboBox<String> statusBox;

    public StudentPanel(Student student, Scheduler scheduler, LoginFrame loginFrame)
    {
        this.student = student;
        this.scheduler = scheduler;
        this.loginFrame = loginFrame;
        predictionService = new PredictionService();
        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        tabs = new JTabbedPane();
        tabs.addTab("Home", makeHome());
        tabs.addTab("My Jobs", makeJobs());
        panel.add(tabs, BorderLayout.CENTER);
        JPanel bottom = new JPanel();
        JButton logoutBtn = new JButton("Log Out");
        bottom.add(logoutBtn);
        panel.add(bottom, BorderLayout.SOUTH);
        logoutBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                logout();
            }
        });
        tabs.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e)
            {
                refreshJobs();
                refreshAllowance();
            }
        });
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
        top.setLayout(new GridLayout(4, 1));
        top.add(new JLabel("Welcome, " + student.getName()));
        top.add(new JLabel("University ID: " + student.getUniId()));
        clockLabel = new JLabel("");
        top.add(clockLabel);
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
        minsLabel = new JLabel("Duration: " + getMinutes() + " GPU minutes");
        form.add(minsLabel);
        home.add(form, BorderLayout.CENTER);
        sizeBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                minsLabel.setText("Duration: " + getMinutes() + " GPU minutes");
            }
        });
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
        top.add(new JLabel("Search ID, title or application:"));
        searchField = new JTextField(12);
        top.add(searchField);
        top.add(new JLabel("Status:"));
        String[] statuses = {"All", "QUEUED", "RUNNING", "COMPLETED", "CANCELLED"};
        statusBox = new JComboBox<String>(statuses);
        top.add(statusBox);
        JButton searchBtn = new JButton("Search");
        top.add(searchBtn);
        jobs.add(top, BorderLayout.NORTH);
        String[] cols = {"Job ID", "Title", "Application", "Status", "GPU", "Duration (min)", "Queue Position", "Start / Prediction", "Finish / Prediction", "Wait (min)", "Remaining (min)", "Result"};
        jobsModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int col)
            {
                return false;
            }
        };
        jobsTable = new JTable(jobsModel);
        jobsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jobsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int i = 0; i < cols.length; i++)
        {
            jobsTable.getColumnModel().getColumn(i).setPreferredWidth(125);
        }
        jobsTable.getColumnModel().getColumn(7).setPreferredWidth(155);
        jobsTable.getColumnModel().getColumn(8).setPreferredWidth(155);
        jobsTable.getColumnModel().getColumn(11).setPreferredWidth(230);
        jobs.add(new JScrollPane(jobsTable), BorderLayout.CENTER);
        JPanel bottom = new JPanel();
        JButton refreshBtn = new JButton("Refresh");
        JButton detailsBtn = new JButton("Job Details");
        JButton cancelBtn = new JButton("Cancel Job");
        bottom.add(refreshBtn);
        bottom.add(detailsBtn);
        bottom.add(cancelBtn);
        jobs.add(bottom, BorderLayout.SOUTH);
        searchBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                refreshJobs();
            }
        });
        searchField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                refreshJobs();
            }
        });
        statusBox.addActionListener(new ActionListener() {
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
        detailsBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                showDetails();
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
        if (scheduler.getClock() == null || scheduler.getClock().getCurrentTime() == null)
        {
            clockLabel.setText("Simulated time: Not set by administrator yet");
        }
        else
        {
            clockLabel.setText("Simulated time: " + timeText(scheduler.getClock().getCurrentTime()));
        }
        if (student.getTimeAllowance() == null)
        {
            allowLabel.setText("GPU Allowance: Not available");
            return;
        }
        allowLabel.setText("Allocated: " + student.getTimeAllowance().getAllocated() + " | Used: " + student.getTimeAllowance().getUsed() + " | Reserved: " + student.getTimeAllowance().getReserved() + " | Available: " + student.getTimeAllowance().getAvailable() + " minutes");
    }

    private int getMinutes()
    {
        String size = (String) sizeBox.getSelectedItem();
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
        return mins;
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
        if (scheduler.getClock() == null || scheduler.getClock().getCurrentTime() == null)
        {
            JOptionPane.showMessageDialog(panel, "The administrator must set the simulation clock first.");
            return;
        }
        int mins = getMinutes();
        if (student.getTimeAllowance() == null || student.getTimeAllowance().getAvailable() < mins)
        {
            JOptionPane.showMessageDialog(panel, "Not enough available GPU minutes.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(panel, "This job will take " + mins + " GPU minutes. Submit?", "Confirm Job", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION)
        {
            try
            {
                Job job = scheduler.submitJob(student.getUniId(), title, app, desc, size);
                nameField.setText("");
                appField.setText("");
                descField.setText("");
                refreshJobs();
                refreshAllowance();
                JOptionPane.showMessageDialog(panel, "Job submitted successfully. Job ID: " + job.getJobId());
                loginFrame.saveData();
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
        ArrayList<PredictionService.JobPrediction> predictions = getPredictions();
        String search = searchField.getText().trim();
        String status = (String) statusBox.getSelectedItem();
        for (int i = 0; i < jobs.size(); i++)
        {
            Job job = jobs.get(i);
            if (job.getStudentId().equals(student.getUniId()))
            {
                boolean found = search.isEmpty() || job.getJobId().equalsIgnoreCase(search) || job.getTitle().equalsIgnoreCase(search) || job.getApplicationName().equalsIgnoreCase(search);
                if (found && (status.equals("All") || job.getStatus().equals(status)))
                {
                    String gpu = "None";
                    String queue = "-";
                    String start = timeText(job.getStartTime());
                    String finish = timeText(job.getFinishTime());
                    String wait = "0";
                    String remaining = "-";
                    if (job.getAssignedGpuID() != null)
                    {
                        gpu = "GPU " + job.getAssignedGpuID();
                    }
                    if (job.getStatus().equals("QUEUED"))
                    {
                        wait = "-";
                        remaining = "" + job.getEstimatedDuration();
                        for (int j = 0; j < predictions.size(); j++)
                        {
                            PredictionService.JobPrediction prediction = predictions.get(j);
                            if (prediction.getJobId().equals(job.getJobId()))
                            {
                                queue = "" + prediction.getQueuePosition();
                                start = timeText(prediction.getPredictedStart());
                                finish = timeText(prediction.getPredictedFinish());
                                wait = "" + prediction.getRemainingWaitMinutes();
                                break;
                            }
                        }
                    }
                    else if (job.getStatus().equals("RUNNING"))
                    {
                        finish = timeText(job.getExpectedFinishTime());
                        remaining = "" + job.getRemainingMinutes(scheduler.getClock().getCurrentTime());
                    }
                    Object[] row = {job.getJobId(), job.getTitle(), job.getApplicationName(), job.getStatus(), gpu, job.getEstimatedDuration(), queue, start, finish, wait, remaining, resultText(job)};
                    jobsModel.addRow(row);
                }
            }
        }
    }

    private ArrayList<PredictionService.JobPrediction> getPredictions()
    {
        if (scheduler.getClock() == null || scheduler.getClock().getCurrentTime() == null)
        {
            return new ArrayList<PredictionService.JobPrediction>();
        }
        return predictionService.getPredictions(
            scheduler.getGpus(), scheduler.getQueue(), scheduler.getClock().getCurrentTime()
        );
    }

    private String timeText(LocalDateTime time)
    {
        if (time == null)
        {
            return "-";
        }
        return ValidationService.formatSimulatedTime(time);
    }

    private String resultText(Job job)
    {
        if (job.getStatus().equals("COMPLETED"))
        {
            return "Completed, " + job.getEstimatedDuration() + " minutes charged";
        }
        if (job.getStatus().equals("CANCELLED"))
        {
            return "Cancelled, " + job.getElapsedMinutes(job.getCancellationTime()) + " minutes charged";
        }
        return "-";
    }

    private Job getSelectedJob()
    {
        int row = jobsTable.getSelectedRow();
        if (row == -1)
        {
            JOptionPane.showMessageDialog(panel, "Select a job first.");
            return null;
        }
        String jobId = jobsModel.getValueAt(row, 0).toString();
        Job job = scheduler.findJob(jobId);
        if (job == null || !job.getStudentId().equals(student.getUniId()))
        {
            JOptionPane.showMessageDialog(panel, "Job not found in your account.");
            refreshJobs();
            return null;
        }
        return job;
    }

    private void showDetails()
    {
        Job job = getSelectedJob();
        if (job == null)
        {
            return;
        }
        String gpu = "None";
        if (job.getAssignedGpuID() != null)
        {
            gpu = "GPU " + job.getAssignedGpuID();
        }
        String details = "Job ID: " + job.getJobId() + "\nTitle: " + job.getTitle() + "\nApplication: " + job.getApplicationName() + "\nDescription: " + job.getDescription() + "\nTask size: " + job.getTaskSize() + "\nDuration: " + job.getEstimatedDuration() + " minutes" + "\nStatus: " + job.getStatus() + "\nAssigned GPU: " + gpu + "\nSubmitted: " + timeText(job.getSubmissionTime()) + "\nStarted: " + timeText(job.getStartTime()) + "\nFinished: " + timeText(job.getFinishTime());
        if (job.getStatus().equals("QUEUED"))
        {
            ArrayList<PredictionService.JobPrediction> predictions = getPredictions();
            for (int i = 0; i < predictions.size(); i++)
            {
                PredictionService.JobPrediction prediction = predictions.get(i);
                if (prediction.getJobId().equals(job.getJobId()))
                {
                    details += "\nQueue position: " + prediction.getQueuePosition() + "\nPredicted GPU: " + prediction.getPredictedGpuId() + "\nPredicted start: " + timeText(prediction.getPredictedStart()) + "\nPredicted finish: " + timeText(prediction.getPredictedFinish()) + "\nRemaining wait: " + prediction.getRemainingWaitMinutes() + " minutes";
                    break;
                }
            }
        }
        else if (job.getStatus().equals("RUNNING"))
        {
            details += "\nPredicted finish: " + timeText(job.getExpectedFinishTime()) + "\nRemaining runtime: " + job.getRemainingMinutes(scheduler.getClock().getCurrentTime()) + " minutes";
        }
        if (job.getStatus().equals("CANCELLED"))
        {
            details += "\nCancelled: " + timeText(job.getCancellationTime()) + "\nCancelled by: " + job.getCancelledBy() + "\nReason: " + job.getCancellationReason();
        }
        details += "\nResult: " + resultText(job);
        JTextArea text = new JTextArea(details, 20, 50);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        JOptionPane.showMessageDialog(panel, new JScrollPane(text), "Job Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void cancelJob()
    {
        Job job = getSelectedJob();
        if (job == null)
        {
            return;
        }
        if (!job.getStatus().equals("QUEUED") && !job.getStatus().equals("RUNNING"))
        {
            JOptionPane.showMessageDialog(panel, "Only queued or running jobs can be cancelled.");
            return;
        }
        if (scheduler.getClock() == null || scheduler.getClock().getCurrentTime() == null)
        {
            JOptionPane.showMessageDialog(panel, "The simulation clock is not initialized.");
            return;
        }
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
        long charge = job.getElapsedMinutes(scheduler.getClock().getCurrentTime());
        int confirm = JOptionPane.showConfirmDialog(panel, "Cancel " + job.getJobId() + "?\nGPU minutes charged: " + charge + "\nUnused reserved minutes released: " + (job.getEstimatedDuration() - charge), "Confirm Cancellation", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
        {
            return;
        }
        try
        {
            int used = student.getTimeAllowance().getUsed();
            scheduler.cancelJob(job.getJobId(), student, reason.trim());
            int charged = student.getTimeAllowance().getUsed() - used;
            refreshJobs();
            refreshAllowance();
            JOptionPane.showMessageDialog(panel, "Job cancelled. " + charged + " GPU minutes charged.");
            loginFrame.saveData();
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

    private void logout()
    {
        if (loginFrame.saveData())
        {
            loginFrame.showLogin();
        }
    }
}
