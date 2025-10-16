package com.thatameliah.SmartCardRegister.Forms;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class MainRegisterUI extends JFrame {
    private JButton QuitButton;
    private JPanel ContentPane;

    private boolean isFullscreen = false;
    private Rectangle windowedBounds;

    // Main form constructor
    public MainRegisterUI() {
        setTitle("Register");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 300);
        setContentPane(ContentPane);

        // Setup Content Pane focusing
        ContentPane.setFocusable(true);
        ContentPane.requestFocus();

        // Setup click behaviour for the Quit button
        QuitButton.addActionListener(event -> Quit());

        // Setup keybind behaviour
        ContentPane.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent event) {
                int keyCode = event.getKeyCode();
                switch (keyCode) {
                    // F11 - fullscreen
                    case (KeyEvent.VK_F11):
                        toggleFullscreen();
                        break;
                }
            }
        });
    }

    private void toggleFullscreen() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (!isFullscreen) {
            // Enter fullscreen
            windowedBounds = getBounds();
            dispose();
            setUndecorated(true);
            setVisible(true);
            device.setFullScreenWindow(this);
            isFullscreen = true;
        } else {
            // Exit fullscreen
            device.setFullScreenWindow(null);
            dispose();
            setUndecorated(false);
            setBounds(windowedBounds);
            setVisible(true);
            isFullscreen = false;
        }
    }

    // Quit function - bound to the quit button
    public void Quit() {
        System.exit(0);
    }
}