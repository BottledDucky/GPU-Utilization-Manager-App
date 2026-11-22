package ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import logic.AccountService;

public class RegistrationPanel {

    private AccountService accountService;
    private LoginFrame loginFrame;
    private JPanel panel;
    private JTextField uniIdField;
    private JTextField nameField;
    private JTextField emailField;
    private JTextField phoneField;
    private JPasswordField passField;
    private JLabel msgLabel;

    public RegistrationPanel(AccountService accountService, LoginFrame loginFrame)
    {
        this.accountService = accountService;
        this.loginFrame = loginFrame;

        panel = new JPanel();
        panel.setLayout(new GridLayout(8, 2, 10, 10));

        panel.add(new JLabel("University ID:"));
        uniIdField = new JTextField();
        panel.add(uniIdField);

        panel.add(new JLabel("Full Name:"));
        nameField = new JTextField();
        panel.add(nameField);

        panel.add(new JLabel("Email:"));
        emailField = new JTextField();
        panel.add(emailField);

        panel.add(new JLabel("Phone Number:"));
        phoneField = new JTextField();
        panel.add(phoneField);

        panel.add(new JLabel("Password:"));
        passField = new JPasswordField();
        panel.add(passField);

        JButton registerBtn = new JButton("Submit Request");
        panel.add(registerBtn);

        JButton backBtn = new JButton("Back to Login");
        panel.add(backBtn);

        msgLabel = new JLabel("");
        panel.add(msgLabel);

        registerBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                registerStudent();
            }
        });

        backBtn.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                RegistrationPanel.this.loginFrame.showLogin();
            }
        });
    }

    public JPanel getPanel()
    {
        return panel;
    }

    private void registerStudent()
    {
        String uniId = uniIdField.getText();
        String name = nameField.getText();
        String email = emailField.getText();
        String phoneNum = phoneField.getText();
        String password = new String(passField.getPassword());

        try
        {
            accountService.registerStudent(uniId, name, email, phoneNum, password);

            msgLabel.setForeground(Color.GREEN);
            msgLabel.setText("Request sent. Status: Pending.");

            passField.setText("");

            if (loginFrame.saveData())
            {
                JOptionPane.showMessageDialog(panel, "Registration request submitted successfully.");
            }
        }
        catch (IllegalArgumentException e)
        {
            msgLabel.setForeground(Color.RED);
            msgLabel.setText(e.getMessage());
        }
    }

}
