package com.thatameliah.SmartCardRegister.Forms;

import com.thatameliah.SmartCardRegister.Handlers.*;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.JSpinner.DateEditor;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class Register extends JFrame {
    private JPanel ContentPane;
    private JLabel StatusLabel;
    private JLabel ReaderLabel;
    private JComboBox<PresenceState> SetPresenceBox;
    private JList<String> PersonList;
    private JButton FileButton;
    private JButton EditButton;
    private JButton StudentButton;
    private JButton ViewButton;
    private JLabel StartTimeLabel;

    private final DefaultListModel<String> personListModel;

    private final Map<Integer, String> people = new HashMap<>();
    private final Map<Integer, PresenceState> presenceStates = new HashMap<>();

    private int nextID = 1;

    private boolean isFullscreen = false;
    private Rectangle windowedBounds;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
    private Date startTime;

    public enum Status {
        LOADING,
        READY,
        WORKING,
        AWAITING_INPUT,
        AWAITING_FILE,
        SAVING_FILE,
        LOADING_FILE,
    }

    public enum PresenceState {
        PRESENT,
        ABSENT,
        LATE,
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

    private final Map<PresenceState, Color> PRESENCE_MAP = new HashMap<>() {{
        put(PresenceState.PRESENT, Color.GREEN);
        put(PresenceState.ABSENT, Color.PINK);
        put(PresenceState.LATE, Color.ORANGE);
    }};

    // Main form constructor
    public Register() {
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
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                Quit();
            }
        });

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
                String entry = (String) value;

                int id = ParseIdFromListString(entry);
                PresenceState state = presenceStates.getOrDefault(id, PresenceState.ABSENT);

                if (isSelected) {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                } else {
                    label.setBackground(PRESENCE_MAP.get(state));
                    label.setForeground(Color.BLACK);
                }

                label.setOpaque(true);
                return label;
            }
        });

        SetPresenceBox.setModel(new DefaultComboBoxModel<>(PresenceState.values()));
        PersonList.addListSelectionListener(e -> UpdateFieldsFromSelection());
        SetPresenceBox.addActionListener(e -> UpdatePresence());

        // Setup keybinds
        // Assign keys to actions
        enum Action {
            EXIT(KeyEvent.VK_ESCAPE),
            FULLSCREEN(KeyEvent.VK_F11),
            NEW_PERSON(KeyEvent.VK_N),
            DELETE(KeyEvent.VK_DELETE),
            SAVE(KeyEvent.VK_S),
            OPEN(KeyEvent.VK_O),
            PRESENT(KeyEvent.VK_1),
            LATE(KeyEvent.VK_2),
            ABSENT(KeyEvent.VK_3),
            TOGGLE_PRESENT(KeyEvent.VK_ENTER);

            final int keyCode;

            Action(int keyCode) {
                this.keyCode = keyCode;
            }
        }

        // Assign actions to functions
        record KeyBinding(Action action, int modifiers, String name, Runnable handler) {}

        final List<KeyBinding> keybinds = List.of(
                // JFrame actions
                new KeyBinding(Action.EXIT, 0, "Exit", this::Quit),
                new KeyBinding(Action.FULLSCREEN, 0, "ToggleFullscreen", this::ToggleFullscreen),

                // Register management actions
                new KeyBinding(Action.NEW_PERSON, KeyEvent.CTRL_DOWN_MASK, "NewPerson", this::NewPerson),
                new KeyBinding(Action.DELETE, 0, "DeleteSelected", () -> DeletePerson(false)),
                new KeyBinding(Action.DELETE, KeyEvent.CTRL_DOWN_MASK, "SudoDeleteSelected", () -> DeletePerson(true)),

                // File management actions
                new KeyBinding(Action.SAVE, KeyEvent.CTRL_DOWN_MASK, "SaveRegister", this::SaveRegister),
                new KeyBinding(Action.OPEN, KeyEvent.CTRL_DOWN_MASK, "OpenRegister", this::LoadRegister),

                // Person management actions
                new KeyBinding(Action.PRESENT, KeyEvent.ALT_DOWN_MASK, "SetPresent", () -> SetPresence(PresenceState.PRESENT)),
                new KeyBinding(Action.LATE, KeyEvent.ALT_DOWN_MASK, "SetLate", () -> SetPresence(PresenceState.LATE)),
                new KeyBinding(Action.ABSENT, KeyEvent.ALT_DOWN_MASK, "SetAbsent", () -> SetPresence(PresenceState.ABSENT)),
                new KeyBinding(Action.TOGGLE_PRESENT, KeyEvent.CTRL_DOWN_MASK, "TogglePresent", this::TogglePresent)
        );

        // Bind functions to keypresses
        for (var kb : keybinds) {
            BindKey(kb.action.keyCode, kb.modifiers, kb.name, kb.handler);
        }

        // Setup complete - set status to READY
        SetStatus(Status.READY);
    }

    // Helper Functions
    public void SetStatus(Status status) {
        String message = STATUS_MAP.getOrDefault(status, "Unknown");
        StatusLabel.setText("Status: " + message);
    }

    private void BindKey(int keyCode, int modifiers, String actionName, Runnable function) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);
        if (keyStroke == null) {
            System.err.println("Invalid KeyStroke: " + keyCode);
            return;
        }

        InputMap inputMap = ContentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = ContentPane.getActionMap();

        Object oldBinding = inputMap.get(keyStroke);
        if (oldBinding != null) {
            inputMap.remove(keyStroke);
            actionMap.remove(oldBinding);
        }

        inputMap.put(keyStroke, actionName);
        actionMap.put(actionName, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                try { function.run(); }
                catch (Exception ex) { System.err.println(ex.getMessage()); }
            }
        });
    }

    private void AddMenuActions(List<JMenuItem> items, Runnable... actions) {
        for (int i = 0; i < items.size() && i < actions.length; i++) {
            final int INDEX = i;
            items.get(i).addActionListener(event -> actions[INDEX].run());
        }
    }

    private File LoadFileFromSystem() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("./saves"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Register Save Files", "rsave"));

        SetStatus(Status.AWAITING_FILE);
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    private File SaveFileToSystem() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("./saves"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Register Save Files", "rsave"));
        fileChooser.setDialogTitle("Save As");

        SetStatus(Status.AWAITING_FILE);
        int result = fileChooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }

    private String FormatListString(int id, String name) {
        return "[" + id + "]" + ": " + name;
    }

    private int ParseIdFromListString(@NotNull String entry) {
        return Integer.parseInt(entry.split(":")[0].replaceAll("[\\[\\]]","").trim());
    }

    // Button press functions
    private void NewPerson() {
        SetStatus(Status.AWAITING_INPUT);

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField forenameField = new JTextField();
        JTextField surnameField = new JTextField();
        forenameField.setFont(new Font("JetBrains Mono", Font.PLAIN, 14));
        surnameField.setFont(new Font("JetBrains Mono", Font.PLAIN, 14));

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

                if (people.containsValue(fullName)) {
                    int duplicateEntry = JOptionPane.showConfirmDialog(
                            this,
                            "This person already exists. Do you wish to continue?",
                            "Duplicate Entry",
                            JOptionPane.YES_NO_OPTION
                    );

                    if (duplicateEntry == JOptionPane.NO_OPTION) {
                        SetStatus(Status.READY); return;
                    }
                }

                int id = nextID++;
                people.put(id, fullName);
                presenceStates.put(id, PresenceState.ABSENT);
                personListModel.addElement(FormatListString(id, fullName));
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

    private void UpdatePerson() {
        SetStatus(Status.AWAITING_INPUT);

        int selectedIndex = PersonList.getSelectedIndex();
        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a person to update.",
                    "No Person Selected",
                    JOptionPane.WARNING_MESSAGE
            );
            SetStatus(Status.READY);
            return;
        }

        SetStatus(Status.WORKING);

        String entry = personListModel.getElementAt(selectedIndex);

        int id = ParseIdFromListString(entry);
        String OldFullName = people.get(id);
        String[] parts = OldFullName.split("", 2);
        String oldForename = parts[0].replaceFirst(" ","");
        String oldSurname = parts[1].replaceFirst(" ","");

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField forenameField = new JTextField(oldForename);
        JTextField surnameField = new JTextField(oldSurname);
        forenameField.setFont(new Font("JetBrains Mono", Font.PLAIN, 14));
        surnameField.setFont(new Font("JetBrains Mono", Font.PLAIN, 14));

        inputPanel.add(new JLabel("Forename:"));
        inputPanel.add(forenameField);
        inputPanel.add(new JLabel("Surname:"));
        inputPanel.add(surnameField);

        int result = JOptionPane.showConfirmDialog(
                this,
                inputPanel,
                "Update Person",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            SetStatus(Status.READY);
            return;
        }

        SetStatus(Status.WORKING);
        String newForename = forenameField.getText().trim();
        String newSurname = surnameField.getText().trim();

        if (newForename.isEmpty() || newSurname.isEmpty()) {
            SetStatus(Status.AWAITING_INPUT);
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot update person: Forename and/or surname field is empty.",
                    "Update Failed",
                    JOptionPane.WARNING_MESSAGE
            );
            SetStatus(Status.READY);
            return;
        }

        String newFullName = newForename + " " + newSurname;
        if (newFullName.equals(OldFullName)) {
            SetStatus(Status.AWAITING_INPUT);
            JOptionPane.showMessageDialog(
                    this,
                    "Cannot update person: No changes were made.",
                    "Update Failed",
                    JOptionPane.INFORMATION_MESSAGE
            );
            SetStatus(Status.READY);
            return;
        }

        people.put(id, newFullName);
        personListModel.set(selectedIndex, FormatListString(id, newFullName));
        SetStatus(Status.READY);
    }

    private void DeletePerson(boolean OverrideWarning) {
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

        String entry = personListModel.getElementAt(selectedIndex);
        int id = ParseIdFromListString(entry);
        String selectedName = people.get(id);

        if (!OverrideWarning) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to delete \"" + selectedName + "\" (ID: " + id + ")? This action cannot be undone!",
                    "Confirm Delete",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (confirm != JOptionPane.YES_OPTION) {
                SetStatus(Status.READY);
                return;
            }
        }

        SetStatus(Status.WORKING);
        people.remove(id);
        presenceStates.remove(id);
        personListModel.remove(selectedIndex);
        SetStatus(Status.READY);
    }

    private void UpdateStartTime() {
        SpinnerDateModel model = new SpinnerDateModel();
        JSpinner timeSpinner = new JSpinner(model);

        DateEditor dateEditor = new DateEditor(timeSpinner, "HH:mm");
        timeSpinner.setEditor(dateEditor);

        SetStatus(Status.AWAITING_INPUT);
        int result = JOptionPane.showConfirmDialog(
                this,
                timeSpinner,
                "Set Start Time",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            SetStatus(Status.READY);
            return;
        }
        Date selectedTime = model.getDate();
        Calendar today = Calendar.getInstance();
        Calendar selectedCal = Calendar.getInstance();
        selectedCal.setTime(selectedTime);

        today.set(Calendar.HOUR_OF_DAY, selectedCal.get(Calendar.HOUR_OF_DAY));
        today.set(Calendar.MINUTE, selectedCal.get(Calendar.MINUTE));
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        startTime = today.getTime();


        StartTimeLabel.setText("Start Time: " + dateFormat.format(startTime));
    }

    private void UpdateFieldsFromSelection() {
        int selectedIndex = PersonList.getSelectedIndex();
        if (selectedIndex == -1) { return; }

        String entry = personListModel.getElementAt(selectedIndex);
        int id = ParseIdFromListString(entry);

        PresenceState state = presenceStates.getOrDefault(id, PresenceState.ABSENT);
        SetPresenceBox.setSelectedItem(state);
    }

    private void UpdatePresence() {
        int selectedIndex = PersonList.getSelectedIndex();
        if (selectedIndex == -1) { return; }

        String entry = personListModel.getElementAt(selectedIndex);
        int id = ParseIdFromListString(entry);

        PresenceState state = (PresenceState) SetPresenceBox.getSelectedItem();
        if (state == null) { return; }

        presenceStates.put(id, state);
        PersonList.revalidate();
        PersonList.repaint();
    }

    private void TogglePresent() {
        int selectedIndex = PersonList.getSelectedIndex();
        if (selectedIndex == -1) { return; }

        String entry = personListModel.getElementAt(selectedIndex);
        int id = ParseIdFromListString(entry);

        PresenceState currentState = presenceStates.getOrDefault(id, PresenceState.ABSENT);
        PresenceState newState;

        Date now = new Date();
        if (startTime != null && now.before(startTime)) {
            newState = (currentState == PresenceState.PRESENT) ? PresenceState.ABSENT : PresenceState.PRESENT;
        } else {
            newState = (currentState == PresenceState.LATE) ? PresenceState.ABSENT : PresenceState.LATE;
        }

        presenceStates.put(id, newState);
        SetPresenceBox.setSelectedItem(newState);

        PersonList.revalidate();
        PersonList.repaint();
    }

    private void SetPresence(PresenceState newPresence) {
        SetPresenceBox.setSelectedItem(newPresence);
    }

    // For saving and exiting, we don't want the form to quit out if the saving fails, so this function returns an integer.
    // A 0 means the save was successful, a 1 means the save was unsuccessful
    // For the Quit() function call only, this return value is checked and the quitting is aborted if the save fails.
    private int SaveRegister() {
        File selectedFile = SaveFileToSystem();
        if (selectedFile == null) {
            SetStatus(Status.READY);
            return 1;
        }

        SetStatus(Status.SAVING_FILE);

        if (selectedFile.exists()) {
            int overwriteResponse = JOptionPane.showConfirmDialog(
                    this,
                    "File " + selectedFile.getName() + " already exists. Saving this register under that filename will overwrite that file. Do you wish to continue?",
                    "Overwrite File",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (overwriteResponse == JOptionPane.NO_OPTION) {
                SetStatus(Status.READY);
                return 1;
            }
        }

        try {
            JSONObject[] peopleObjects = people.entrySet().stream()
                    .map(entry -> JSONHandler.CreatePersonJSON(entry.getValue(), entry.getKey().toString()))
                    .toArray(JSONObject[]::new);

            JSONArray jsonArray = JSONHandler.ToJSONArray(peopleObjects);

            JSONObject startTimeObject = new JSONObject();
            startTimeObject.put("StartTime", startTime != null ? dateFormat.format(startTime) : "Ctrl + E");
            jsonArray.put(startTimeObject);

            String jsonString = JSONHandler.ToJSONString(jsonArray, 4);
            FileHandler.WriteToFile(Base64Handler.EncodeString(jsonString), selectedFile);

            JOptionPane.showMessageDialog(
                    this,
                    "Register saved successfully!",
                    "Save successful",
                    JOptionPane.INFORMATION_MESSAGE
            );
        } catch (Exception err) {
            JOptionPane.showMessageDialog(
                    this,
                    "Error saving register: " + err.getMessage(),
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.out.println(err.getMessage());
            return 1;
        }
        SetStatus(Status.READY);
        return 0;
    }

    private void LoadRegister() {
        File loadedFile = LoadFileFromSystem();
        if (loadedFile == null) {
            SetStatus(Status.READY);
            return;
        }

        try {
            String encodedString = FileHandler.ReadFile(loadedFile);
            if (encodedString == null || encodedString.isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "The selected file could not be read or is empty.",
                        "Load Error",
                        JOptionPane.ERROR_MESSAGE
                );
                SetStatus(Status.READY);
                return;
            }

            String jsonString = Base64Handler.DecodeString(encodedString);
            JSONArray jsonArray = JSONHandler.parseJSONArray(jsonString);

            people.clear();
            presenceStates.clear();
            personListModel.clear();
            nextID = 1;
            startTime = null;

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                if (obj.has("StartTime")) {
                    String timeString = obj.getString("StartTime");
                    try {
                        Date timeOnly = dateFormat.parse(timeString);
                        Calendar today = Calendar.getInstance();
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(timeOnly);

                        today.set(Calendar.HOUR_OF_DAY, cal.get(Calendar.HOUR_OF_DAY));
                        today.set(Calendar.MINUTE, cal.get(Calendar.MINUTE));
                        today.set(Calendar.SECOND, 0);
                        today.set(Calendar.MILLISECOND, 0);

                        startTime = today.getTime();
                    } catch (ParseException err) {
                        startTime = null;
                    }
                    StartTimeLabel.setText("Start Time: " + (startTime != null ? dateFormat.format(startTime) : "Ctrl + E"));
                    continue;
                }

                int id = Integer.parseInt(obj.getString("id"));
                String name = obj.getString("name");

                people.put(id, name);
                personListModel.addElement(FormatListString(id, name));

                if (id >= nextID) { nextID = id + 1; }
            }
        } catch (Exception err) {
            JOptionPane.showMessageDialog(
                    this,
                    "Error loading register \"" + loadedFile.getName() + "\": " + err.getMessage(),
                    "Load Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.out.println(err.getMessage());
        }
        SetStatus(Status.READY);
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
                int saveResult = SaveRegister();
                if (saveResult == 1) {
                    SetStatus(Status.READY);
                    return;
                }
                break;

            case JOptionPane.NO_OPTION:
                break; // Do nothing, exit as usual

            case JOptionPane.CANCEL_OPTION:
                SetStatus(Status.READY); // Cancel quitting
                return;
        }
        System.exit(0);
    }

    // Some components require custom creation instead of the preset creation from the Swing UI Designer plugin
    // This function handles those cases and will run first, as the form is loading
    private void createUIComponents() {
        // Ensure ContentPane exists
        if (ContentPane == null) {
            ContentPane = new JPanel();
        }

        // Create UI Panels
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Create buttons for drop-down menus
        final List<JMenuItem> fileMenuItems = List.of(
                new JMenuItem("Save (Ctrl + S)"),
                new JMenuItem("Open File (Ctrl + O)"),
                new JMenuItem("Exit (Escape)")
        );

        final List<JMenuItem> editMenuItems = List.of(
                new JMenuItem("Set start time (Ctrl + E)"),
                new JMenuItem("Set student Present (Alt + 1)"),
                new JMenuItem("Set student Late (Alt + 2)"),
                new JMenuItem("Set student Absent (Alt + 3)")
        );

        final List<JMenuItem> viewMenuItems = List.of(
                new JMenuItem("Toggle Fullscreen (F11)")
        );

        final List<JMenuItem> studentMenuItems = List.of(
                new JMenuItem("New student (Ctrl + N)"),
                new JMenuItem("Edit selected student (Ctrl + E)"),
                new JMenuItem("Delete selected student (Delete)"),
                new JMenuItem("Toggle student present (Ctrl + Enter)")
        );

        // Create popup menus
        JPopupMenu filePopupMenu = new JPopupMenu();
        AddMenuActions(fileMenuItems,
                this::SaveRegister,
                this::LoadRegister,
                this::Quit
        );
        fileMenuItems.forEach(filePopupMenu::add);

        JPopupMenu editPopupMenu = new JPopupMenu();
        AddMenuActions(editMenuItems,
                this::UpdateStartTime,
                () -> SetPresence(PresenceState.PRESENT),
                () -> SetPresence(PresenceState.LATE),
                () -> SetPresence(PresenceState.ABSENT)
        );
        editMenuItems.forEach(editPopupMenu::add);

        JPopupMenu viewPopupMenu = new JPopupMenu();
        AddMenuActions(viewMenuItems,
                this::ToggleFullscreen
        );
        viewMenuItems.forEach(viewPopupMenu::add);

        JPopupMenu studentPopupMenu = new JPopupMenu();
        AddMenuActions(studentMenuItems,
                this::NewPerson,
                this::UpdatePerson,
                () -> DeletePerson(false),
                this::TogglePresent
        );
        studentMenuItems.forEach(studentPopupMenu::add);

        // Create drop down buttons
        FileButton = new JButton("\uD83D\uDCC1 File");
        FileButton.setFont(new Font("JetBrains Mono", Font.BOLD, 14));
        FileButton.setFocusPainted(false);
        FileButton.setBackground(new Color(240, 240, 240));
        FileButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        FileButton.addActionListener(event -> filePopupMenu.show(FileButton, 0, FileButton.getHeight()));

        EditButton = new JButton("\uD83D\uDCDD Edit");
        EditButton.setFont(new Font("JetBrains Mono", Font.BOLD, 14));
        EditButton.setFocusPainted(false);
        EditButton.setBackground(new Color(240, 240, 240));
        EditButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        EditButton.addActionListener(event -> editPopupMenu.show(EditButton, 0, EditButton.getHeight()));

        ViewButton = new JButton("\uD83D\uDC41 View");
        ViewButton.setFont(new Font("JetBrains Mono", Font.BOLD, 14));
        ViewButton.setFocusPainted(false);
        ViewButton.setBackground(new Color(240, 240, 240));
        ViewButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        ViewButton.addActionListener(event -> viewPopupMenu.show(ViewButton, 0, ViewButton.getHeight()));

        StudentButton = new JButton("\uD83D\uDC64 Student");
        StudentButton.setFont(new Font("JetBrains Mono", Font.BOLD, 14));
        StudentButton.setFocusPainted(false);
        StudentButton.setBackground(new Color(240, 240, 240));
        StudentButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        StudentButton.addActionListener(event -> studentPopupMenu.show(StudentButton, 0, StudentButton.getHeight()));

        // Create Status label
        StatusLabel = new JLabel("Status: READY");
        StatusLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 20));
        StatusLabel.setPreferredSize(new Dimension(200, 30));
        StatusLabel.setForeground(Color.BLACK);
        statusPanel.add(StatusLabel, BorderLayout.CENTER);

        // Create NFCReader name label
        ReaderLabel = new JLabel("Reader: N/A");
        ReaderLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 20));
        ReaderLabel.setPreferredSize(new Dimension(200, 30));
        ReaderLabel.setForeground(Color.BLACK);
        statusPanel.add(ReaderLabel, BorderLayout.CENTER);

        // Create Start Time Label
        StartTimeLabel = new JLabel("Start Time: HH:MM");
        StartTimeLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 20));
        StartTimeLabel.setPreferredSize(new Dimension(200, 30));
        StartTimeLabel.setForeground(Color.BLACK);
        statusPanel.add(StartTimeLabel, BorderLayout.CENTER);

        // Add panels to main ContentPane
        ContentPane.setLayout(new BorderLayout());
        ContentPane.add(statusPanel, BorderLayout.SOUTH);
    }
}