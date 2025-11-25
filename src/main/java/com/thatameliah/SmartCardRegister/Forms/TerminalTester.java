package com.thatameliah.SmartCardRegister.Forms;

import com.thatameliah.SmartCardRegister.Utils.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TerminalTester extends JFrame {
    private JPanel ContentPane;
    private JLabel TerminalLabel;
    private JLabel UIDLabel;

    private volatile boolean running = false;
    private Thread cardListenerThread;

    public TerminalTester() {
        final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
        final double HEIGHT = SCREEN_SIZE.getHeight();
        final int V_HEIGHT = (int) HEIGHT / 4;
        final double WIDTH = SCREEN_SIZE.getWidth();
        final int V_WIDTH = (int) WIDTH / 4;

        this.setSize(V_WIDTH, V_HEIGHT);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setContentPane(ContentPane);
        this.setTitle("Terminal Tester Utility");

        ContentPane.setFocusable(true);
        ContentPane.requestFocus();

        TerminalLabel.setText("Terminal: " + NFCHandler.GetActiveTerminalName());

        SetupStopOnClose();
        StartCardListener();
    }

    private void StartCardListener() {
        running = true;

        cardListenerThread = new Thread(this::ListenForCards);
        cardListenerThread.setDaemon(true);
        cardListenerThread.start();
    }

    public void StopCardListener() {
        running = false;

        if (cardListenerThread != null) {
            cardListenerThread.interrupt();
        }
    }

    private void ListenForCards() {
        while (running && ContentPane.isVisible()) {
            String UID = NFCHandler.GetUIDFromCard(0);
            SwingUtilities.invokeLater(() -> { UIDLabel.setText("Last UID: " + (UID.isEmpty() ? "N/A" : UID)); });
        }
    }

    public void SetupStopOnClose() {
        this.addComponentListener(new ComponentAdapter() {
            @Override public void componentHidden(ComponentEvent e) { StopCardListener(); }
        });

        this.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { StopCardListener(); }
            @Override public void windowClosed(WindowEvent e) { StopCardListener(); }
        });
    }


    private void createUIComponents() {
        if (ContentPane == null) {
            ContentPane = new JPanel();
        }

        TerminalLabel = new JLabel("Terminal: " + NFCHandler.GetActiveTerminalName());
        TerminalLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 20));
    }
}
