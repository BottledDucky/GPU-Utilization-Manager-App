package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import app.Main;
import logic.AccountService;
import logic.Scheduler;
import logic.PredictionService;
import logic.UtilizationService;
import model.Student;
import model.Administrator;

public class LoginFrame {

    private JFrame frame;
    private JPanel panel;
    private JTextField usernameField;
    private JPasswordField passField;
    private JLabel msgLabel;
    private AccountService accountService;
    private Scheduler scheduler;

    public LoginFrame(AccountService accountService, Scheduler scheduler)
    {
        this.accountService = accountService;
        this.scheduler = scheduler;

        frame = new JFrame("GPU Utilization Manager");
        frame.setSize(500, 350);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e)
            {
                if (saveData())
                {
                    frame.dispose();
                }
            }
        });

        panel = new JPanel();
        panel.setLayout(new GridLayout(5, 2, 10, 10));

        panel.add(new JLabel("Username / University ID:"));
        usernameField = new JTextField();
        panel.add(usernameField);

        panel.add(new JLabel("Password:"));
        passField = new JPasswordField();
        panel.add(passField);

        JButton loginBtn = new JButton("Login");
        panel.add(loginBtn);

        JButton registerBtn = new JButton("Request an Account");
        panel.add(registerBtn);

        msgLabel = new JLabel("");
        panel.add(msgLabel);

        frame.add(panel);

        loginBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                login();
            }
        });

        registerBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                openRegistration();
            }
        });

        frame.setVisible(true);
    }

    private void login()
    {
        String username = usernameField.getText();
        String password = new String(passField.getPassword());

        try
        {
            if (username.equals("admin"))
            {
                Administrator admin = accountService.loginAdmin(username, password);

                if (!setupClock())
                {
                    return;
                }

                RealAdminBackend backend = new RealAdminBackend(scheduler, accountService, admin,
                    new Runnable() {
                        public void run()
                        {
                            saveData();
                        }
                    },
                    new Runnable() {
                        public void run()
                        {
                            showLogin();
                        }
                    }
                );

                PredictionService predictionService = new PredictionService();
                UtilizationService utilizationService = new UtilizationService();

                AdminDashboard dashboard = new AdminDashboard(
                    backend, predictionService, utilizationService
                );

                dashboard.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

                dashboard.addWindowListener(new WindowAdapter() {
                    public void windowClosing(WindowEvent e)
                    {
                        if (saveData())
                        {
                            dashboard.dispose();
                            frame.dispose();
                        }
                    }
                });

                passField.setText("");
                frame.setVisible(false);
                dashboard.setVisible(true);
            }
            else
            {
                Student student = accountService.loginStudent(username, password);
                StudentPanel studentPanel = new StudentPanel(student, scheduler, this);

                passField.setText("");
                frame.getContentPane().removeAll();
                frame.add(studentPanel.getPanel());
                frame.setSize(1100, 650);
                frame.setLocationRelativeTo(null);
                frame.revalidate();
                frame.repaint();
            }
        }
        catch (IllegalArgumentException e)
        {
            msgLabel.setForeground(Color.RED);
            msgLabel.setText(e.getMessage());
        }
        catch (IllegalStateException e)
        {
            msgLabel.setForeground(Color.RED);
            msgLabel.setText(e.getMessage());
        }
    }

    private boolean setupClock()
    {
        if (scheduler.getClock().isInitialized())
        {
            return true;
        }

        JTextField yearField = new JTextField();
        JTextField monthField = new JTextField();
        JTextField dayField = new JTextField();
        JTextField hourField = new JTextField();
        JTextField minuteField = new JTextField();

        JPanel clockPanel = new JPanel(new GridLayout(5, 2, 5, 5));

        clockPanel.add(new JLabel("Year:"));
        clockPanel.add(yearField);
        clockPanel.add(new JLabel("Month:"));
        clockPanel.add(monthField);
        clockPanel.add(new JLabel("Day:"));
        clockPanel.add(dayField);
        clockPanel.add(new JLabel("Hour (0-23):"));
        clockPanel.add(hourField);
        clockPanel.add(new JLabel("Minute (0-59):"));
        clockPanel.add(minuteField);

        while (true)
        {
            int choice = JOptionPane.showConfirmDialog(frame, clockPanel, "Set simulation clock", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (choice != JOptionPane.OK_OPTION)
            {
                return false;
            }

            try
            {
                int year = Integer.parseInt(yearField.getText());
                int month = Integer.parseInt(monthField.getText());
                int day = Integer.parseInt(dayField.getText());
                int hour = Integer.parseInt(hourField.getText());
                int minute = Integer.parseInt(minuteField.getText());

                LocalDateTime time = LocalDateTime.of(year, month, day, hour, minute);

                scheduler.getClock().initialize(time);

                return saveData();
            }
            catch (NumberFormatException e)
            {
                JOptionPane.showMessageDialog(frame, "Please enter numbers only.");
            }
            catch (java.time.DateTimeException e)
            {
                JOptionPane.showMessageDialog(frame, "Invalid date or time.");
            }
        }
    }

    private void openRegistration()
    {
        RegistrationPanel registration = new RegistrationPanel(accountService, this);

        frame.getContentPane().removeAll();
        frame.add(registration.getPanel());
        frame.revalidate();
        frame.repaint();
    }

    public void showLogin()
    {
        frame.getContentPane().removeAll();
        frame.add(panel);

        usernameField.setText("");
        passField.setText("");
        msgLabel.setText("");

        frame.setSize(500, 350);
        frame.setLocationRelativeTo(null);
        frame.revalidate();
        frame.repaint();
        frame.setVisible(true);
    }

    public boolean saveData()
    {
        try
        {
            Main.saveData();
            return true;
        }
        catch (RuntimeException e)
        {
            JOptionPane.showMessageDialog(frame, "The changes are still open, but could not be saved.\n" + e.getMessage(), "Save failed", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

}