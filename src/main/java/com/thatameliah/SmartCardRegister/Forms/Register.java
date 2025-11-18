package com.thatameliah.SmartCardRegister.Forms;

import com.thatameliah.SmartCardRegister.Utils.*;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private JComboBox<Presence> SetPresenceBox;
    private JList<String> StudentList;
    private JButton FileButton;
    private JButton EditButton;
    private JButton StudentButton;
    private JButton ViewButton;
    private JLabel StartTimeLabel;

    private final int MAX_NAME_LENGTH = 25;

    private final DefaultListModel<String> STUDENT_LIST_MODEL;

    private final Map<Integer, String> STUDENTS = new HashMap<>();
    private final Map<Integer, Presence> PRESENCE_STATES = new HashMap<>();

    private record Shortcut(int keyCode, int modifiers, String name, Runnable handler) {}

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
                new Shortcut(KeyEvent.VK_ESCAPE, 0, "Exit", this::Quit),
                new Shortcut(KeyEvent.VK_F11, 0, "ToggleFullscreen", this::ToggleFullscreen),

                // Register management actions
                new Shortcut(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK, "NewStudent", this::NewStudent),
                new Shortcut(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK, "UpdateStudent", this::UpdateStudent),
                new Shortcut(KeyEvent.VK_DELETE, 0, "DeleteSelected", () -> DeleteStudent(false)),
                new Shortcut(KeyEvent.VK_DELETE, KeyEvent.CTRL_DOWN_MASK, "SudoDeleteSelected", () -> DeleteStudent(true)),
                new Shortcut(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK, "SetStartTime", this::UpdateStartTime),

                // File management actions
                new Shortcut(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, "SaveRegister", this::SaveRegister),
                new Shortcut(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK, "OpenRegister", this::LoadRegister),

                // Student management actions
                new Shortcut(KeyEvent.VK_1, KeyEvent.ALT_DOWN_MASK, "SetPresent", () -> SetPresence(Presence.PRESENT)),
                new Shortcut(KeyEvent.VK_2, KeyEvent.ALT_DOWN_MASK, "SetLate", () -> SetPresence(Presence.LATE)),
                new Shortcut(KeyEvent.VK_3, KeyEvent.ALT_DOWN_MASK, "SetAbsent", () -> SetPresence(Presence.ABSENT)),
                new Shortcut(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK, "TogglePresent", this::TogglePresent)
        };

        // Bind functions to key presses
        for (var shortcut : SHORTCUTS) { BindKey(shortcut); }

        // Setup complete - set status to READY
        SetStatus(Status.READY);
    }

    // Helper Functions
    public void SetStatus(Status status) {
        String message = STATUS_MAP.getOrDefault(status, "Unknown");
        StatusLabel.setText("Status: " + message);
    }

    /**
     * Binds a shortcut's KeyCode and Modifiers to its Runnable function
     *
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
     *
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

    private void UpdateFieldsFromSelection() {
        int selectedIndex = StudentList.getSelectedIndex();
        if (selectedIndex == -1) { return; }

        String entry = STUDENT_LIST_MODEL.getElementAt(selectedIndex);
        int id = ParseIdFromListString(entry);

        Presence state = PRESENCE_STATES.getOrDefault(id, Presence.ABSENT);
        SetPresenceBox.setSelectedItem(state);
    }

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

    private void TogglePresent() {
        int selectedIndex = StudentList.getSelectedIndex();
        if (selectedIndex == -1) { return; }

        String entry = STUDENT_LIST_MODEL.getElementAt(selectedIndex);
        int id = ParseIdFromListString(entry);

        Presence currentState = PRESENCE_STATES.getOrDefault(id, Presence.ABSENT);
        Presence newState;

        Date now = new Date();
        if (startTime != null && now.before(startTime)) {
            newState = (currentState == Presence.PRESENT) ? Presence.ABSENT : Presence.PRESENT;
        } else {
            newState = (currentState == Presence.LATE) ? Presence.ABSENT : Presence.LATE;
        }

        PRESENCE_STATES.put(id, newState);
        SetPresenceBox.setSelectedItem(newState);

        StudentList.revalidate();
        StudentList.repaint();
    }

    private void SetPresence(Presence newPresence) {
        SetPresenceBox.setSelectedItem(newPresence);
    }

    /**
     * Saves the currently open register to a file
     *
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
        int saveResult = JOptionPane.showConfirmDialog(
                this,
                "Do you wish to save the current register?",
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

        // "File" drop down button
        FileButton = new JButton("File");
        FileButton.setFont(new Font("JetBrains Mono", Font.BOLD, 14));
        FileButton.setFocusPainted(false);
        FileButton.setBackground(new Color(240, 240, 240));
        FileButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        FileButton.addActionListener(event -> filePopupMenu.show(FileButton, 0, FileButton.getHeight()));

        // "Edit" drop down button
        EditButton = new JButton("Edit");
        EditButton.setFont(new Font("JetBrains Mono", Font.BOLD, 14));
        EditButton.setFocusPainted(false);
        EditButton.setBackground(new Color(240, 240, 240));
        EditButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        EditButton.addActionListener(event -> editPopupMenu.show(EditButton, 0, EditButton.getHeight()));

        // "View" drop down button
        ViewButton = new JButton("View");
        ViewButton.setFont(new Font("JetBrains Mono", Font.BOLD, 14));
        ViewButton.setFocusPainted(false);
        ViewButton.setBackground(new Color(240, 240, 240));
        ViewButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        ViewButton.addActionListener(event -> viewPopupMenu.show(ViewButton, 0, ViewButton.getHeight()));

        // "Student" drop down button
        StudentButton = new JButton("Student");
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