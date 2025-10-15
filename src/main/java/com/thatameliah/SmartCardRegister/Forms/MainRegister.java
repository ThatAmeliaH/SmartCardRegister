package com.thatameliah.SmartCardRegister.Forms;

import javax.swing.*;

public class MainRegister extends JFrame {
    private JButton QuitButton;
    private JPanel ContentPane;

    // Main form constructor
    public MainRegister() {
        setTitle("Register");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 300);
        setContentPane(ContentPane);

        // Setup click behaviour for the Quit button
        QuitButton.addActionListener(event -> Quit());
    }

    // Quit function - bound to the quit button
    public void Quit() {
        System.exit(0);
    }
}