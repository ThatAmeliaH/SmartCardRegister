package com.thatameliah.SmartCardRegister.Forms;

import javax.swing.*;
import javax.swing.plaf.FileChooserUI;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class MainRegisterUI extends JFrame {
    private final Dimension SCREENSIZE = Toolkit.getDefaultToolkit().getScreenSize();
    private final double WIDTH = SCREENSIZE.getWidth();
    private final double HEIGHT = SCREENSIZE.getHeight();

    private JButton QuitButton;
    private JPanel ContentPane;

    private boolean isFullscreen = false;
    private Rectangle windowedBounds;

    // Main form constructor
    public MainRegisterUI() {
        // Setup view size
        final int V_HEIGHT = (int) HEIGHT / 2;
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

        // Setup click behaviour for the Quit button
        QuitButton.addActionListener(event -> Quit());

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
    }

    private void toggleFullscreen() {
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        
        if (isFullscreen) {
            // Exit fullscreen
            device.setFullScreenWindow(null);
            dispose();
            setUndecorated(false);
            setBounds(windowedBounds);
            isFullscreen = false;
        } else {
            // Enter fullscreen
            windowedBounds = getBounds();
            dispose();
            setUndecorated(true);
            device.setFullScreenWindow(this);
            isFullscreen = true;
        }
    }

    public void Quit() {
        // TODO: Make a "Do you wish to save current register?" popup
        System.exit(0);
    }
}