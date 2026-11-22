package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import logic.AccountService;
import model.Student;
import model.Administrator;

public class LoginFrame {

    private JFrame frame;
    private JPanel panel;
    private JTextField usernameField;
    private JPasswordField passField;
    private JLabel msgLabel;
    private AccountService accountService;

    public LoginFrame(AccountService accountService)
    {
        this.accountService = accountService;

        frame = new JFrame("GPU Utilization Manager");
        frame.setSize(500, 350);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

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

                JOptionPane.showMessageDialog(frame, "Welcome, Administrator!");

                // Connect to Member 3's AdminDashboard later.
            }
            else
            {
                Student student = accountService.loginStudent(username, password);

                JOptionPane.showMessageDialog(frame, "Welcome, " + student.getName() + "!");

                // Connect to StudentPanel later.
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

    private void openRegistration()
    {
        RegistrationPanel registration = new RegistrationPanel(accountService);

        frame.getContentPane().removeAll();

        frame.add(registration.getPanel());

        frame.revalidate();
        frame.repaint();
    }

}