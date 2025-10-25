package com.thatameliah.SmartCardRegister.Forms;

import javax.swing.*;
import javax.swing.filechooser.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MainRegisterUI extends JFrame {
    private JPanel ContentPane;
    private JButton LoadButton;
    private JLabel StatusLabel;
    private JList<String> PersonList;
    private JButton NewPersonButton;
    private JButton DeleteSelectedButton;
    private JButton togglePresentButton;

    private final DefaultListModel<String> personListModel;
    private final Set<String> presentPeople = new HashSet<>();

    private boolean isFullscreen = false;
    private Rectangle windowedBounds;

    public enum Status {
        LOADING,
        READY,
        WORKING,
        AWAITING_INPUT,
        AWAITING_FILE,
        SAVING_FILE,
        LOADING_FILE,
    }

    private final Map<Status, String> STATUS_MAP = new HashMap<>() {{
        put(Status.READY, "Ready");
        put(Status.LOADING, "Loading");
        put(Status.WORKING, "Working");
        put(Status.AWAITING_INPUT, "Awaiting Input");
        put(Status.AWAITING_FILE, "Awaiting File");
        put(Status.SAVING_FILE, "Saving File");
        put(Status.LOADING_FILE, "Loading File");
    }};

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

        // Initialise list model
        personListModel = new DefaultListModel<>();
        PersonList.setModel(personListModel);
        PersonList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String name = (String) value;
                if (presentPeople.contains(name)) {
                    label.setBackground(Color.GREEN.brighter());
                } else {
                    label.setBackground(Color.WHITE);
                }

                return label;
            }
        });

        // Setup button behaviours
        NewPersonButton.addActionListener(event -> NewPerson());                // Create new person
        DeleteSelectedButton.addActionListener(event -> DeletePerson());        // Delete selected person
        togglePresentButton.addActionListener(event -> TogglePersonPresent());  // Toggle person present/absent
        LoadButton.addActionListener(event -> LoadRegister());                  // Load register from file

        // Setup key press behaviour
        //InputMap inputMap = ContentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        //ActionMap actionMap = ContentPane.getActionMap();

        final String EXIT_KEY = "ESCAPE";
        final String FULLSCREEN_KEY = "F11";
        final String DELETE_SELECTED_KEY = "DELETE";

        BindKey(ContentPane, EXIT_KEY, "Exit", this::Quit);
        BindKey(ContentPane, FULLSCREEN_KEY, "ToggleFullscreen", this::ToggleFullscreen);
        BindKey(ContentPane, DELETE_SELECTED_KEY, "DeleteSelected", this::DeletePerson);

        SetStatus(Status.READY);
    }

    // Helper Functions
    public void SetStatus(Status status) {
        String message = STATUS_MAP.getOrDefault(status, "Unknown");
        StatusLabel.setText("Status: " + message);
    }

    private void BindKey(JComponent component, String keyStrokeString, String actionName, Runnable action) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyStrokeString);
        if (keyStroke == null) {
            System.err.println("Invalid KeyStroke: " + keyStrokeString);
            return;
        }

        InputMap inputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = component.getActionMap();

        Object oldBinding = inputMap.get(keyStroke);
        if (oldBinding != null) {
            inputMap.remove(keyStroke);
            actionMap.remove(oldBinding);
        }

        inputMap.put(keyStroke, actionName);
        actionMap.put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    action.run();
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        });
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

    // Button press functions
    private void NewPerson() {
        SetStatus(Status.AWAITING_INPUT);

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField forenameField = new JTextField();
        JTextField surnameField = new JTextField();

        inputPanel.add(new JLabel("Forename:"));
        inputPanel.add(forenameField);
        inputPanel.add(new JLabel("Surname:"));
        inputPanel.add(surnameField);

        int result = JOptionPane.showConfirmDialog(
                this,
                inputPanel,
                "New Person",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        SetStatus(Status.WORKING);

        if (result == JOptionPane.OK_OPTION) {
            String forename = forenameField.getText().trim();
            String surname = surnameField.getText().trim();

            if (!forename.isEmpty() && !surname.isEmpty()) {
                String fullName = forename + " " + surname;
                personListModel.addElement(fullName);
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Please enter both a forename and a surname.",
                        "Incomplete Information",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        }

        SetStatus(Status.READY);
    }

    private void DeletePerson() {
        SetStatus(Status.AWAITING_INPUT);

        int selectedIndex = PersonList.getSelectedIndex();

        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "No person is selected to be deleted.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE
            );
            SetStatus(Status.READY);
            return;
        }

        String selectedName = personListModel.getElementAt(selectedIndex);
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to delete \"" + selectedName + "\"? This action cannot be undone!",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            SetStatus(Status.READY);
            return;
        }

        SetStatus(Status.WORKING);
        personListModel.remove(selectedIndex);
        SetStatus(Status.READY);
    }

    private void TogglePersonPresent() {
        int selectedIndex = PersonList.getSelectedIndex();

        SetStatus(Status.AWAITING_INPUT);
        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "No person selected.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE
            );
            SetStatus(Status.READY);
            return;
        }

        SetStatus(Status.WORKING);
        String selectedName = personListModel.getElementAt(selectedIndex);

        if (presentPeople.contains(selectedName)) {
            presentPeople.remove(selectedName);
        } else {
            presentPeople.add(selectedName);
        }

        PersonList.repaint();
        SetStatus(Status.READY);
    }

    private void LoadRegister() {
        File loadedFile = LoadFileFromSystem();
        if (loadedFile == null) { return; }
    }

    private void ToggleFullscreen() {
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
                SetStatus(Status.READY); // Cancel quitting
                return;
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
        StatusLabel.setPreferredSize(new Dimension(200, 30));
        StatusLabel.setForeground(Color.BLACK);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(StatusLabel, BorderLayout.CENTER);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        ContentPane.setLayout(new BorderLayout());
        ContentPane.add(statusPanel, BorderLayout.SOUTH);
    }
}