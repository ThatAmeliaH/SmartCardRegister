package com.thatameliah.SmartCardRegister.Forms;

import javax.swing.*;

public class LoadingScreen extends JFrame {
    private JPanel ContentPane;
    private JLabel LoadingLabel;

    public LoadingScreen() {
        final int V_HEIGHT = 200;
        final int V_WIDTH = 400;

        setSize(V_WIDTH, V_HEIGHT);
        setUndecorated(true);
        setAlwaysOnTop(true);
        setLocationRelativeTo(null);

        setContentPane(ContentPane);

        setVisible(true);
    }
}