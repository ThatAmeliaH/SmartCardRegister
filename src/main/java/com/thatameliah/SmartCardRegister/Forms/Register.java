package com.thatameliah.SmartCardRegister.Forms;

import com.thatameliah.SmartCardRegister.Utils.*;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.smartcardio.*;

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
    private JLabel TerminalLabel;
    private JComboBox<Presence> SetPresenceBox;
    private JList<String> StudentList;
    private JButton FileButton;
    private JButton EditButton;
    private JButton StudentButton;
    private JButton TerminalButton;
    private JButton ViewButton;
    private JLabel StartTimeLabel;

    private final int MAX_NAME_LENGTH = 25;

    private final DefaultListModel<String> STUDENT_LIST_MODEL;

    private final Map<Integer, String> STUDENTS = new HashMap<>();
    private final Map<Integer, Presence> PRESENCE_STATES = new HashMap<>();

    private record Shortcut(String name, int keyCode, int modifiers, Runnable handler) {}

    private volatile boolean Listening = false;
    private Thread CardListenerThread;
    private int nextID = 1;

    private boolean isFullscreen = false;
    private Rectangle windowedBounds;

    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm");
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

    public enum Presence {
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

    private final Map<Presence, Color> PRESENCE_MAP = new HashMap<>() {{
        put(Presence.PRESENT, new Color(150, 255, 150));
        put(Presence.ABSENT, new Color(255, 150, 200));
        put(Presence.LATE, new Color(255, 200, 50));
    }};
    
    public Status status;

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
        this.setTitle("Register");
        this.setSize(V_WIDTH, V_HEIGHT);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setLocationRelativeTo(null);
        this.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                Quit();
            }
        });

        // Content Pane configuration
        this.setContentPane(ContentPane);
        ContentPane.setFocusable(true);
        ContentPane.requestFocus();

        // Initialise list model
        STUDENT_LIST_MODEL = new DefaultListModel<>();
        StudentList.setModel(STUDENT_LIST_MODEL);
        StudentList.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String entry = (String) value;

                int id = ParseIdFromListString(entry);
                Presence state = PRESENCE_STATES.getOrDefault(id, Presence.ABSENT);

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

        SetPresenceBox.setModel(new DefaultComboBoxModel<>(Presence.values()));
        StudentList.addListSelectionListener(e -> UpdateFieldsFromSelection());
        SetPresenceBox.addActionListener(e -> UpdatePresence());

        // Setup shortcuts
        // Assign keys to functions
        final Shortcut[] SHORTCUTS = {
                // JFrame actions
                new Shortcut("Exit", KeyEvent.VK_ESCAPE, 0, this::Quit),
                new Shortcut("ToggleFullscreen", KeyEvent.VK_F11, 0, this::ToggleFullscreen),

                // Register management actions
                new Shortcut("NewStudent", KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK, this::NewStudent),
                new Shortcut("UpdateStudent", KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK, this::UpdateStudent),
                new Shortcut("DeleteSelected", KeyEvent.VK_DELETE, 0, () -> DeleteStudent(false)),
                new Shortcut("SudoDeleteSelected", KeyEvent.VK_DELETE, KeyEvent.CTRL_DOWN_MASK, () -> DeleteStudent(true)),
                new Shortcut("SetStartTime", KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK, this::UpdateStartTime),

                // File management actions
                new Shortcut("SaveRegister", KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, this::SaveRegister),
                new Shortcut("OpenRegister", KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK, this::LoadRegister),

                // Student management actions
                new Shortcut("SetPresent", KeyEvent.VK_1, KeyEvent.ALT_DOWN_MASK, () -> SetPresence(Presence.PRESENT)),
                new Shortcut("SetLate", KeyEvent.VK_2, KeyEvent.ALT_DOWN_MASK, () -> SetPresence(Presence.LATE)),
                new Shortcut("SetAbsent", KeyEvent.VK_3, KeyEvent.ALT_DOWN_MASK, () -> SetPresence(Presence.ABSENT)),
                new Shortcut("TogglePresent", KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK, this::TogglePresent),

                // Terminal management actions
                new Shortcut("SetActiveTerminal", KeyEvent.VK_T, KeyEvent.SHIFT_DOWN_MASK, this::SetActiveTerminal),
                new Shortcut("RefreshConnectedTerminals", KeyEvent.VK_R, KeyEvent.SHIFT_DOWN_MASK, NFCHandler::RefreshTerminals),
                new Shortcut("OpenTerminalTester", KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK, this::OpenTerminalTester)
        };

        // Bind functions to key presses
        for (var shortcut : SHORTCUTS) { BindKey(shortcut); }

        // Setup terminal and begin listening for cards.
        TerminalLabel.setText("Terminal: " + NFCHandler.GetActiveTerminalName());
        SetupStopOnClose();
        StartCardListener();
        
        // Setup complete - set status to READY
        SetStatus(Status.READY);
    }

    // Helper Functions
    public void SetStatus(Status status) {
        this.status = status;
        
        String message = STATUS_MAP.getOrDefault(status, "Unknown");
        StatusLabel.setText("Status: " + message);
    }

    /**
     * Binds a shortcut's KeyCode and Modifiers to its Runnable function
     * @param shortcut The shortcut to bind
     */
    private void BindKey(@NotNull Shortcut shortcut) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(shortcut.keyCode, shortcut.modifiers);
        if (keyStroke == null) {
            System.err.println("Invalid KeyStroke: " + shortcut.keyCode);
            return;
        }

        // Gets the input map and action map for the main content pane
        InputMap inputMap = ContentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = ContentPane.getActionMap();

        // Check for existing bindings and overwrite if present
        Object oldBinding = inputMap.get(keyStroke);
        if (oldBinding != null) {
            inputMap.remove(keyStroke);
            actionMap.remove(oldBinding);
        }

        // Add the new bindings to the input and action maps
        inputMap.put(keyStroke, shortcut.name);
        actionMap.put(shortcut.name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                try { shortcut.handler.run(); }
                catch (Exception ex) { System.err.println(ex.getMessage()); }
            }
        });
    }

    /**
     * Link a list of menu items to an array of runnable functions.
     * @param items The list of items to bind the functions to
     * @param actions The array of runnable functions to bind
     */
    private void AddMenuActions(@NotNull List<JMenuItem> items, Runnable... actions) {
        if (items.size() != actions.length) {
            System.err.println("Size mismatch: List of size " + items.size() + " bound to runnable array of length " + actions.length);
        }

        for (int i = 0; i < items.size() && i < actions.length; i++) {
            final int INDEX = i;
            items.get(i).addActionListener(event -> actions[INDEX].run());
        }
    }

    private @Nullable File GetFileFromSystem(String title) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("./saves"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Register Save Files", "rsave"));
        fileChooser.setDialogTitle(title);

        SetStatus(Status.AWAITING_FILE);
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) { return fileChooser.getSelectedFile(); }

        return null;
    }

    // List string functions
    @Contract(pure = true)
    private @NotNull String FormatListString(int id, String name) { return "[" + id + "]" + ": " + name; }
    
    @Contract(pure = true)
    private @NotNull Integer ParseIdFromListString(@NotNull String entry) { return Integer.parseInt(entry.split(":")[0].replaceAll("[\\[\\]]","").trim()); }

    // Button functions
    private void NewStudent() {
        SetStatus(Status.AWAITING_INPUT);

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField forenameField = new JTextField();
        JTextField surnameField = new JTextField();
        forenameField.setFont(new Font("Liberation Sans", Font.PLAIN, 14));
        surnameField.setFont(new Font("Liberation Sans", Font.PLAIN, 14));

        inputPanel.add(new JLabel("Forename:"));
        inputPanel.add(forenameField);
        inputPanel.add(new JLabel("Surname:"));
        inputPanel.add(surnameField);

        int result = JOptionPane.showConfirmDialog(
                this,
                inputPanel,
                "New Student",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        SetStatus(Status.WORKING);

        if (result != JOptionPane.OK_OPTION) {
            SetStatus(Status.READY);
            return;
        }

        String forename = forenameField.getText().trim();
        String surname = surnameField.getText().trim();

        if (forename.length() > MAX_NAME_LENGTH || surname.length() > MAX_NAME_LENGTH) {
            JOptionPane.showMessageDialog(
                    this,
                    (forename.length() > MAX_NAME_LENGTH ? "Forename" : "Surname") + " is too long. Inputs cannot be greater than " + MAX_NAME_LENGTH + " characters.",
                    "Invalid Input",
                    JOptionPane.WARNING_MESSAGE
            );
            SetStatus(Status.READY);
            return;
        }

        if (forename.isEmpty() || surname.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    (forename.isEmpty() ? "Forename" : "Surname") + " cannot be empty.",
                    "Incomplete Information",
                    JOptionPane.WARNING_MESSAGE
            );
            SetStatus(Status.READY);
            return;
        }

        String fullName = forename + " " + surname;

        if (STUDENTS.containsValue(fullName)) {
            int duplicateEntry = JOptionPane.showConfirmDialog(
                    this,
                    "A student already exists with this name. Do you wish to continue?",
                    "Duplicate Entry",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (duplicateEntry == JOptionPane.NO_OPTION) {
                SetStatus(Status.READY); return;
            }
        }

        int id = nextID++;
        STUDENTS.put(id, fullName);
        PRESENCE_STATES.put(id, Presence.ABSENT);
        STUDENT_LIST_MODEL.addElement(FormatListString(id, fullName));

        SetStatus(Status.READY);
    }

    private void UpdateStudent() {
        SetStatus(Status.AWAITING_INPUT);

        int selectedIndex = StudentList.getSelectedIndex();
        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a student to update.",
                    "No Student Selected",
                    JOptionPane.WARNING_MESSAGE
            );
            SetStatus(Status.READY);
            return;
        }

        SetStatus(Status.WORKING);

        String entry = STUDENT_LIST_MODEL.getElementAt(selectedIndex);

        int id = ParseIdFromListString(entry);
        String OldFullName = STUDENTS.get(id);
        String[] parts = OldFullName.split(" ", 2);
        String oldForename = parts[0].replaceFirst(" ","");
        String oldSurname = parts[1].replaceFirst(" ","");

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        JTextField forenameField = new JTextField(oldForename);
        JTextField surnameField = new JTextField(oldSurname);
        forenameField.setFont(new Font("Liberation Sans", Font.PLAIN, 14));
        surnameField.setFont(new Font("Liberation Sans", Font.PLAIN, 14));

        inputPanel.add(new JLabel("Forename:"));
        inputPanel.add(forenameField);
        inputPanel.add(new JLabel("Surname:"));
        inputPanel.add(surnameField);

        int result = JOptionPane.showConfirmDialog(
                this,
                inputPanel,
                "Update Student",
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
                    "Cannot update student: Forename and/or surname field is empty.",
                    "Update Failed",
                    JOptionPane.WARNING_MESSAGE
            );
            SetStatus(Status.READY);
            return;
        }

        if (newForename.length() > MAX_NAME_LENGTH || newSurname.length() > MAX_NAME_LENGTH) {
            JOptionPane.showMessageDialog(
                    this,
                    "Forename or surname is too long. Inputs cannot be greater than " + MAX_NAME_LENGTH + " characters.",
                    "Invalid Input",
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
                    "Cannot update student: No changes were made.",
                    "Update Failed",
                    JOptionPane.INFORMATION_MESSAGE
            );
            SetStatus(Status.READY);
            return;
        }

        if (STUDENTS.containsValue(newFullName)) {
            int duplicateEntry = JOptionPane.showConfirmDialog(
                    this,
                    "A student already exists with this name. Do you wish to continue?",
                    "Duplicate Entry",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );

            if (duplicateEntry == JOptionPane.NO_OPTION) {
                SetStatus(Status.READY); return;
            }
        }

        STUDENTS.put(id, newFullName);
        STUDENT_LIST_MODEL.set(selectedIndex, FormatListString(id, newFullName));
        SetStatus(Status.READY);
    }

    private void DeleteStudent(boolean OverrideWarning) {
        SetStatus(Status.AWAITING_INPUT);

        int selectedIndex = StudentList.getSelectedIndex();

        if (selectedIndex == -1) {
            JOptionPane.showMessageDialog(
                    this,
                    "No student is selected to be deleted.",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE
            );
            SetStatus(Status.READY);
            return;
        }

        String entry = STUDENT_LIST_MODEL.getElementAt(selectedIndex);
        int id = ParseIdFromListString(entry);
        String selectedName = STUDENTS.get(id);

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
        STUDENTS.remove(id);
        PRESENCE_STATES.remove(id);
        STUDENT_LIST_MODEL.remove(selectedIndex);
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


        StartTimeLabel.setText("Start Time: " + DATE_FORMAT.format(startTime));
    }

    /**
     * Updates SelectPresenceBox to match the presence of the selected student.
     */
    private void UpdateFieldsFromSelection() {
        int selectedIndex = StudentList.getSelectedIndex();
        if (selectedIndex == -1) { return; }

        String entry = STUDENT_LIST_MODEL.getElementAt(selectedIndex);
        int id = ParseIdFromListString(entry);

        Presence state = PRESENCE_STATES.getOrDefault(id, Presence.ABSENT);
        SetPresenceBox.setSelectedItem(state);
    }

    /**
     * Updates the presence state of the selected student to the state in SetPresenceBox
     */
    private void UpdatePresence() {
        int selectedIndex = StudentList.getSelectedIndex();
        if (selectedIndex == -1) { return; }

        String entry = STUDENT_LIST_MODEL.getElementAt(selectedIndex);
        int id = ParseIdFromListString(entry);

        Presence state = (Presence) SetPresenceBox.getSelectedItem();
        if (state == null) { return; }

        PRESENCE_STATES.put(id, state);
        StudentList.revalidate();
        StudentList.repaint();
    }

    /**
     * Toggles the selected student between absent and late/present, depending on StartTime.
     */
    private void TogglePresent() {
        int selectedIndex = StudentList.getSelectedIndex();
        if (selectedIndex == -1) { return; }

        String entry = STUDENT_LIST_MODEL.getElementAt(selectedIndex);
        int id = ParseIdFromListString(entry);

        Presence currentState = PRESENCE_STATES.getOrDefault(id, Presence.ABSENT);
        Presence newState;

        Date now = new Date();
        if (startTime != null && now.before(startTime)) { newState = (currentState == Presence.PRESENT) ? Presence.ABSENT : Presence.PRESENT; }
        else { newState = (currentState == Presence.LATE) ? Presence.ABSENT : Presence.LATE; }

        PRESENCE_STATES.put(id, newState);
        SetPresenceBox.setSelectedItem(newState);

        StudentList.revalidate();
        StudentList.repaint();
    }

    private void SetPresence(Presence newPresence) {
        SetPresenceBox.setSelectedItem(newPresence);
    }

    private void StartCardListener() {
        Listening = true;
        
        CardListenerThread = new Thread(this::ListenForCards);
        CardListenerThread.setDaemon(true);
        CardListenerThread.start();
    }
    
    private void StopCardListener() {
        Listening = false;
        
        if (CardListenerThread != null) {
            CardListenerThread.interrupt();
        }
    }
    
    private void ListenForCards() {
        while (Listening && this.isVisible()) {
            String UID = NFCHandler.GetUIDFromCard(0, status);
            // TODO: Make this toggle the presence of a student.
        }
    }
    
    /**
     * Saves the currently open register to a file
     * @return Whether the save attempt was successful
     */
    private boolean SaveRegister() {
        File selectedFile = GetFileFromSystem("Save As");
        if (selectedFile == null) {
            SetStatus(Status.READY);
            return false;
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
                return false;
            }
        }

        try {
            JSONObject[] peopleObjects = STUDENTS.entrySet().stream()
                    .map(entry -> JSONHandler.CreateStudentJSON(entry.getValue(), entry.getKey().toString()))
                    .toArray(JSONObject[]::new);

            JSONArray jsonArray = JSONHandler.ToJSONArray(peopleObjects);

            JSONObject startTimeObject = new JSONObject();
            startTimeObject.put("StartTime", startTime != null ? DATE_FORMAT.format(startTime) : "Not Set");
            jsonArray.put(startTimeObject);

            String jsonString = JSONHandler.ToJSONString(jsonArray, 4);
            FileHandler.WriteToFile(selectedFile, Base64Handler.EncodeString(jsonString));

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
            return false;
        }
        SetStatus(Status.READY);
        return true;
    }

    private void LoadRegister() {
        File loadedFile = GetFileFromSystem("Load File");
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
            JSONArray jsonArray = JSONHandler.ParseJSONArray(jsonString);

            STUDENTS.clear();
            PRESENCE_STATES.clear();
            STUDENT_LIST_MODEL.clear();
            nextID = 1;
            startTime = null;

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                if (obj.has("StartTime")) {
                    String timeString = obj.getString("StartTime");
                    try {
                        Date timeOnly = DATE_FORMAT.parse(timeString);
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
                    StartTimeLabel.setText("Start Time: " + (startTime != null ? DATE_FORMAT.format(startTime) : "Not Set"));
                    continue;
                }

                int id = Integer.parseInt(obj.getString("id"));
                String name = obj.getString("name");

                STUDENTS.put(id, name);
                STUDENT_LIST_MODEL.addElement(FormatListString(id, name));

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
            this.dispose();
            this.setUndecorated(false);
            this.setBounds(windowedBounds);
            this.setVisible(true);
            isFullscreen = false;
        } else {
            // Enter fullscreen
            windowedBounds = getBounds();
            this.dispose();
            this.setUndecorated(true);
            this.setVisible(true);
            device.setFullScreenWindow(this);
            isFullscreen = true;
        }
    }

    private void SetActiveTerminal() {
        final List<CardTerminal> TERMINALS = NFCHandler.GetConnectedTerminals();
        if (TERMINALS.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No terminals detected. Try refreshing connected terminals with Shift + R.",
                    "No Terminals Detected",
                    JOptionPane.WARNING_MESSAGE
            );
            TerminalLabel.setText("Terminal: N/A");
            return;
        }

        String[] terminalNames = new String[TERMINALS.size()];
        for (int i = 0; i < terminalNames.length; i++) {
            terminalNames[i] = "[" + i + "]: " +  TERMINALS.get(i).getName();
        }

        JComboBox<String> comboBox = new JComboBox<>(terminalNames);

        int result = JOptionPane.showConfirmDialog(
                this,
                comboBox,
                "Select new active terminal",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            int selected = comboBox.getSelectedIndex();
            if (selected >= 0) {
                NFCHandler.SetActiveTerminal(selected);
                TerminalLabel.setText("Terminal: " + NFCHandler.GetActiveTerminalName());
            }
        }
    }

    public void OpenTerminalTester() {
        TerminalTester terminalTester = new TerminalTester(this);
        terminalTester.setVisible(true);
    }
    
    private void SetupStopOnClose() {
        this.addComponentListener(new ComponentAdapter() {
            @Override public void componentHidden(ComponentEvent e) { StopCardListener(); }
        });

        this.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { StopCardListener(); }
            @Override public void windowClosed(WindowEvent e) { StopCardListener(); }
        });
    }

    public void Quit() {
        SetStatus(Status.AWAITING_INPUT);
        int saveResult = JOptionPane.showConfirmDialog(
                this,
                "Do you want to save the current register?",
                "Confirm",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        switch (saveResult) {
            case JOptionPane.YES_OPTION: // Save register and continue exiting
                boolean success = SaveRegister();
                if (success) { break; }

                JOptionPane.showMessageDialog(
                        this,
                        "The current register could not be saved. Ensure the chosen directory exists, or change it, and try again.",
                        "Save Failed",
                        JOptionPane.ERROR_MESSAGE
                );
                SetStatus(Status.READY);
                return;

            case JOptionPane.NO_OPTION: // Do nothing, exit without saving
                break;

            case JOptionPane.CANCEL_OPTION: // Cancel quitting, resume program
                SetStatus(Status.READY);
                return;
        }
        System.exit(0);
    }

    /**
     * Custom create UI components not created by the Swing UI Designer plugin
     */
    private void createUIComponents() {
        // Ensure ContentPane exists
        if (ContentPane == null) {
            ContentPane = new JPanel();
        }

        // Create UI Panels
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // "Menu" drop-down sub buttons:
        final List<JMenuItem> FILE_MENU_ITEMS = List.of(
                new JMenuItem("Save (Ctrl + S)"),
                new JMenuItem("Open File (Ctrl + O)"),
                new JMenuItem("Exit (Escape)")
        );

        // "Edit" drop-down sub buttons:
        final List<JMenuItem> EDIT_MENU_ITEMS = List.of(
                new JMenuItem("Set start time (Ctrl + T)"),
                new JMenuItem("Set student Present (Alt + 1)"),
                new JMenuItem("Set student Late (Alt + 2)"),
                new JMenuItem("Set student Absent (Alt + 3)")
        );

        // "View" drop-down sub buttons:
        final List<JMenuItem> VIEW_MENU_ITEMS = List.of(
                new JMenuItem("Toggle Fullscreen (F11)")
        );

        // "Student" drop-down sub buttons:
        final List<JMenuItem> STUDENT_MENU_ITEMS = List.of(
                new JMenuItem("New student (Ctrl + N)"),
                new JMenuItem("Edit selected student (Ctrl + E)"),
                new JMenuItem("Delete selected student (Delete)"),
                new JMenuItem("Toggle student present (Ctrl + Enter)")
        );

        // "Terminal" drop-down sub buttons:
        final List<JMenuItem> TERMINAL_MENU_ITEMS = List.of(
                new JMenuItem("Select active terminal (Shift + T)"),
                new JMenuItem("Refresh connected terminals (Shift + R)"),
                new JMenuItem("Open Terminal Tester Utility (Ctrl + Alt + T)")
        );

        // "File" button popup menu
        JPopupMenu filePopupMenu = new JPopupMenu();
        AddMenuActions(FILE_MENU_ITEMS,
                this::SaveRegister,
                this::LoadRegister,
                this::Quit
        );
        FILE_MENU_ITEMS.forEach(filePopupMenu::add);

        // "Edit" button popup menu
        JPopupMenu editPopupMenu = new JPopupMenu();
        AddMenuActions(EDIT_MENU_ITEMS,
                this::UpdateStartTime,
                () -> SetPresence(Presence.PRESENT),
                () -> SetPresence(Presence.LATE),
                () -> SetPresence(Presence.ABSENT)
        );
        EDIT_MENU_ITEMS.forEach(editPopupMenu::add);

        // "View" button popup menu
        JPopupMenu viewPopupMenu = new JPopupMenu();
        AddMenuActions(VIEW_MENU_ITEMS,
                this::ToggleFullscreen
        );
        VIEW_MENU_ITEMS.forEach(viewPopupMenu::add);

        // "Student" button popup menu
        JPopupMenu studentPopupMenu = new JPopupMenu();
        AddMenuActions(STUDENT_MENU_ITEMS,
                this::NewStudent,
                this::UpdateStudent,
                () -> DeleteStudent(false),
                this::TogglePresent
        );
        STUDENT_MENU_ITEMS.forEach(studentPopupMenu::add);

        // "Terminal" button popup menu
        JPopupMenu terminalPopupMenu = new JPopupMenu();
        AddMenuActions(TERMINAL_MENU_ITEMS,
                this::SetActiveTerminal,
                NFCHandler::RefreshTerminals,
                this::OpenTerminalTester
        );
        TERMINAL_MENU_ITEMS.forEach(terminalPopupMenu::add);

        // Drop Down Buttons
        FileButton = new JButton("File");
        ConfigureMenuButton(FileButton, filePopupMenu);

        EditButton = new JButton("Edit");
        ConfigureMenuButton(EditButton, editPopupMenu);

        ViewButton = new JButton("View");
        ConfigureMenuButton(ViewButton, viewPopupMenu);

        StudentButton = new JButton("Student");
        ConfigureMenuButton(StudentButton, studentPopupMenu);

        TerminalButton = new JButton("Terminal");
        ConfigureMenuButton(TerminalButton, terminalPopupMenu);

        // Status Labels
        StatusLabel = new JLabel("Status: READY");
        ConfigureStatusLabel(StatusLabel, statusPanel);

        TerminalLabel = new JLabel("Terminal: " + NFCHandler.GetActiveTerminalName());
        ConfigureStatusLabel(TerminalLabel, statusPanel);

        StartTimeLabel = new JLabel("Start Time: HH:MM");
        ConfigureStatusLabel(StartTimeLabel, statusPanel);

        // Add panels to main ContentPane
        ContentPane.setLayout(new BorderLayout());
        ContentPane.add(statusPanel, BorderLayout.SOUTH);
    }

    /**
     * Configure a top bar menu button with an associated drop-down.
     * @param button    The JButton to configure
     * @param popupMenu The JPopupMenu menu to connect the button to
     */
    private void ConfigureMenuButton(@NotNull JButton button, @NotNull JPopupMenu popupMenu) {
        button.setFont(new Font("JetBrains Mono", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBackground(new Color(240, 240, 240));
        button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        button.addActionListener(event -> popupMenu.show(button, 0, button.getHeight()));
    }

    /**
     * Configure and format a JLabel using the standard Status Label format, and link it to a JPanel
     * @param label The label to format
     * @param panel The JPanel to link the formatted label to
     */
    private void ConfigureStatusLabel(JLabel label, JPanel panel) {
        label.setFont(new Font("JetBrains Mono", Font.PLAIN, 20));
        label.setPreferredSize(new Dimension(200, 30));
        label.setForeground(Color.BLACK);
        panel.add(label, BorderLayout.CENTER);
    }
}