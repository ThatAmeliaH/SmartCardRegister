package com.thatameliah.SmartCardRegister.Forms;

import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainRegisterUI extends JFrame {
    private JPanel ContentPane;
    private JButton LoadButton;
    private JLabel StatusLabel;

    private boolean isFullscreen = false;
    private Rectangle windowedBounds;

    public enum Status {
        LOADING,
        READY,
        AWAITING_INPUT,
        AWAITING_FILE,
        SAVING_FILE,
        LOADING_FILE,
    }

    private final Map<Status, String> STATUS_MAP = new HashMap<>() {{
        put(Status.READY, "Ready");
        put(Status.LOADING, "Loading");
        put(Status.AWAITING_INPUT, "Awaiting Input");
        put(Status.AWAITING_FILE, "Awaiting File");
        put(Status.SAVING_FILE, "Saving File");
        put(Status.LOADING_FILE, "Loading File");
    }};

    private Status status;

    // Main form constructor
    public MainRegisterUI() {
        SetStatus(Status.LOADING);

        // Setup view size
        final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();
        final double HEIGHT = SCREEN_SIZE.getHeight();
        final int V_HEIGHT = (int) HEIGHT / 2;
        final double WIDTH = SCREEN_SIZE.getWidth();
        final int V_WIDTH = (int) WIDTH / 2;
        
        // JFrame configuration
        setTitle("Register");
        setSize(V_WIDTH, V_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Content Pane configuration
        setContentPane(ContentPane);
        ContentPane.setFocusable(true);
        ContentPane.requestFocus();

        // Setup load button behaviour
        LoadButton.addActionListener(event -> LoadRegister());

        // Setup key press behaviour
        ContentPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                int keyCode = event.getKeyCode();
                
                switch (keyCode) {
                    case (KeyEvent.VK_F11): // F11 - Fullscreen
                        toggleFullscreen();
                        break;
                        
                    case (KeyEvent.VK_ESCAPE): // ESCAPE - Close
                        Quit();
                        break;
                }
            }
        });

        SetStatus(Status.READY);
    }

    public void SetStatus(Status status) {
        String message = STATUS_MAP.getOrDefault(status, "Unknown");
        StatusLabel.setText("Status: " + message);
    }

    private void LoadRegister() {
        File loadedFile = LoadFileFromSystem();
        if (loadedFile == null) { return; }
    }

    private File LoadFileFromSystem() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("./saves"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Register Save Files", ".rsave"));

        SetStatus(Status.AWAITING_FILE);
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    private void toggleFullscreen() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (isFullscreen) {
            // Exit fullscreen
            device.setFullScreenWindow(null);
            dispose();
            setUndecorated(false);
            setBounds(windowedBounds);
            setVisible(true);
            isFullscreen = false;
        } else {
            // Enter fullscreen
            windowedBounds = getBounds();
            dispose();
            setUndecorated(true);
            setVisible(true);
            device.setFullScreenWindow(this);
            isFullscreen = true;
        }
    }

    public void Quit() {
        SetStatus(Status.AWAITING_INPUT);
        int result = JOptionPane.showConfirmDialog(
                this,
                "Do you wish to save the current register?",
                "Confirm",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        switch (result) {
            case JOptionPane.YES_OPTION:
                SetStatus(Status.SAVING_FILE);
                // TODO: Implement saving
                break;

            case JOptionPane.NO_OPTION:
                break; // Do nothing, exit as usual

            case JOptionPane.CANCEL_OPTION:
                return; // Cancel quitting
        }
        System.exit(0);
    }

    // Some components require custom creation instead of the preset swing UI designer creation
    // This function will run first, as the form is loading
    private void createUIComponents() {
        // Ensure ContentPane exists
        if (ContentPane == null) {
            ContentPane = new JPanel();
        }

        // Custom create Status label
        StatusLabel = new JLabel("Status: READY");
        StatusLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 20));
        StatusLabel.setForeground(Color.BLACK);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(StatusLabel, BorderLayout.EAST);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        ContentPane.setLayout(new BorderLayout());
        ContentPane.add(statusPanel, BorderLayout.SOUTH);
    }
}