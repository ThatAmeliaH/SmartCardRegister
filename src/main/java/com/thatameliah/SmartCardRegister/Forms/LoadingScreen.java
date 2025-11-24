package com.thatameliah.SmartCardRegister.Forms;

import javax.swing.*;

public class LoadingScreen extends JFrame {
    private JPanel ContentPane;
    private JLabel LoadingLabel;

    public LoadingScreen() {
        final int V_HEIGHT = 200;
        final int V_WIDTH = 400;

        this.setSize(V_WIDTH, V_HEIGHT);
        this.setUndecorated(true);
        this.setAlwaysOnTop(true);
        this.setLocationRelativeTo(null);

        this.setContentPane(ContentPane);

        this.setVisible(true);
    }

    private void createUIComponents() {
        if (ContentPane == null) {
            ContentPane = new JPanel();
        }
    }
}