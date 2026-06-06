package com.thatameliah.SmartCardRegister.Forms;

// Internal classes
import com.thatameliah.SmartCardRegister.Exceptions.*;
import com.thatameliah.SmartCardRegister.Utils.*;

// Annotations
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// JSON Objects
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// Smart card integration
import javax.smartcardio.*;

// UI Classes
import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.JSpinner.DateEditor;

// Graphics and rendering
import java.awt.*;
import java.awt.event.*;

// File handling
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;

// Data structures
import java.util.Map;
import java.util.List;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

// Scala Interop
import scala.Option;

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
  private JRadioButton RegisterModeRButton;
  private JRadioButton EditModeRButton;
  private JRadioButton IgnoreButton;

  private final int MAX_NAME_LENGTH = 25;
  
  private final DefaultListModel<String> STUDENT_LIST_MODEL;

  private final Map<Integer, String> STUDENTS = new HashMap<>();
  private final Map<Integer, Presence> PRESENCE_STATES = new HashMap<>();
  private final Map<Integer, String> UNIQUE_IDS = new HashMap<>();

  private record Shortcut(String name, int keyCode, int modifiers, Runnable handler){}
  private record MenuEntry(String label, Runnable action){}

  private volatile boolean Listening = false;
  private Thread CardListenerThread;
  private int nextID = 1;

  public boolean TerminalTesterOpen = false;

  private final InputMap InputMap = ContentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
  private final ActionMap ActionMap = ContentPane.getActionMap();
  
  private boolean isFullscreen = false;
  private Rectangle windowedBounds;

  private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm");
  private Date startTime;

  public TerminalTester terminalTesterWindow;
  public TerminalMode mode = TerminalMode.REGISTER;
  public Status status = Status.LOADING;
  
  public enum Status {
    LOADING,
    READY,
    WORKING,
    AWAITING_INPUT,
    AWAITING_FILE,
    SAVING_FILE,
    LOADING_FILE,
    WAITING_FOR_CARD_ABSENT,
  }

  public enum Presence {
    PRESENT,
    ABSENT,
    LATE,
  }

  public enum TerminalMode {
    REGISTER,
    EDIT,
    IGNORE,
  }

  private final Map<Status, String> STATUS_MAP = new HashMap<>() {{
    put(Status.READY, "Ready");
    put(Status.LOADING, "Loading");
    put(Status.WORKING, "Working");
    put(Status.AWAITING_INPUT, "Awaiting Input");
    put(Status.AWAITING_FILE, "Awaiting File");
    put(Status.SAVING_FILE, "Saving File");
    put(Status.LOADING_FILE, "Loading File");
    put(Status.WAITING_FOR_CARD_ABSENT, "Remove Card");
  }};

  private final Map<Presence, Color> PRESENCE_COLOURS = new HashMap<>() {{
    put(Presence.PRESENT, new Color(150, 255, 150));
    put(Presence.ABSENT, new Color(255, 150, 200));
    put(Presence.LATE, new Color(255, 200, 50));
  }};

  private final Map<Presence, Character> PRESENCE_CHARS = new HashMap<>() {{
    put(Presence.PRESENT, 'O');
    put(Presence.ABSENT, 'X');
    put(Presence.LATE, 'L');
  }};

  // Main form constructor
  public Register() {
    SetStatus(Status.LOADING);
    
    // Calculate window size
    final Toolkit DEFAULT_TOOLKIT = Toolkit.getDefaultToolkit();
    final Dimension SCREEN_SIZE = DEFAULT_TOOLKIT.getScreenSize();
    final double HEIGHT = SCREEN_SIZE.getHeight();
    final int V_HEIGHT = (int) HEIGHT / 2;
    final double WIDTH = SCREEN_SIZE.getWidth();
    final int V_WIDTH = (int) WIDTH / 2;

    // JFrame configuration
    this.setTitle("Register");
    this.setSize(V_WIDTH, V_HEIGHT);
    this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    this.setLocationRelativeTo(null);
    this.addWindowListener(new WindowAdapter() {
      @Override public void windowClosing(WindowEvent e) { Quit(); }
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
          label.setBackground(PRESENCE_COLOURS.get(state));
          label.setForeground(Color.BLACK);
        }

        label.setOpaque(true);
        return label;
      }
    });

    SetPresenceBox.setModel(new DefaultComboBoxModel<>(Presence.values()));
    SetPresenceBox.addActionListener(e -> UpdatePresence());
    StudentList.addListSelectionListener(e -> UpdateFieldsFromSelection());

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
      new Shortcut("SetStartTime", KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK, this::SetStartTime),

      // File management actions
      new Shortcut("SaveRegister", KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK, this::SaveRegister),
      new Shortcut("OpenRegister", KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK, this::LoadRegister),

      // Student management actions
      new Shortcut("SetPresent", KeyEvent.VK_1, KeyEvent.ALT_DOWN_MASK, () -> SetPresence(Presence.PRESENT)),
      new Shortcut("SetLate", KeyEvent.VK_2, KeyEvent.ALT_DOWN_MASK, () -> SetPresence(Presence.LATE)),
      new Shortcut("SetAbsent", KeyEvent.VK_3, KeyEvent.ALT_DOWN_MASK, () -> SetPresence(Presence.ABSENT)),
      new Shortcut("TogglePresent", KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK, this::ToggleSelectedPresence),
      new Shortcut("ShowStudentUIDs", KeyEvent.VK_U, KeyEvent.CTRL_DOWN_MASK, this::ShowStudentUIDs),
      new Shortcut("ResetUID", KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, this::ResetSelectedUID),

      // Terminal management actions
      new Shortcut("SetActiveTerminal", KeyEvent.VK_T, KeyEvent.SHIFT_DOWN_MASK, this::SetActiveTerminal),
      new Shortcut("RefreshConnectedTerminals", KeyEvent.VK_R, KeyEvent.SHIFT_DOWN_MASK, NFCHandler::RefreshTerminals),
      new Shortcut("OpenTerminalTester", KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK, this::OpenTerminalTester)
    };

    // Bind functions to key presses
    for (var shortcut : SHORTCUTS) { BindKey(shortcut); }

    RegisterModeRButton.addActionListener(e -> SetTerminalMode(TerminalMode.REGISTER));
    EditModeRButton.addActionListener(e -> SetTerminalMode(TerminalMode.EDIT));
    IgnoreButton.addActionListener(e -> SetTerminalMode(TerminalMode.IGNORE));

    // Setup terminal and begin listening for cards.
    TerminalLabel.setText("Terminal: " + NFCHandler.GetActiveTerminalName());
    SetupStopOnClose();
    StartCardListenerAsync();

    // Setup complete - set status to READY
    SetStatus(Status.READY);
  }

  // Helper Functions
  /**
   * Sets the status of the register.
   * @param status The status to set the register to.
   */
  public void SetStatus(Status status) {
    this.status = status;

    String message = STATUS_MAP.getOrDefault(status, "Unknown");
    StatusLabel.setText("Status: " + message);
  }

  /**
   * Binds a shortcut's KeyCode and Modifiers to its Runnable function
   * @param shortcut The shortcut to bind
   * @throws InvalidShortcutException If the provided KeyCode has already been bound or does not exist
   */
  private void BindKey(@NotNull Shortcut shortcut) throws InvalidShortcutException {
    KeyStroke keyStroke = KeyStroke.getKeyStroke(shortcut.keyCode, shortcut.modifiers);
    if (keyStroke == null) { throw new InvalidShortcutException("Invalid KeyStroke: Cannot parse KeyStroke for shortcut \"" + shortcut + "\""); }

    // Check for existing bindings and throw an exception if present
    Object oldBinding = InputMap.get(keyStroke);
    if (oldBinding != null) { throw new InvalidShortcutException("Duplicate Shortcut: KeyStroke \"" + keyStroke + "\" already bound to \""+ oldBinding + "\""); }

    // Add the new bindings to the input and action maps
    InputMap.put(keyStroke, shortcut.name);
    ActionMap.put(shortcut.name, new AbstractAction() {
      @Override public void actionPerformed(ActionEvent e) { shortcut.handler.run(); }
    });
  }

  /**
   * Create a popup menu, and link it to a list of menu entries.
   * @param entries The list of entries to link the popup menu to.
   * @return The JPopupMenu, created and formatted
   */
  private @NotNull JPopupMenu BuildMenu(@NotNull MenuEntry... entries) {
    JPopupMenu popup = new JPopupMenu();
    
    for (MenuEntry entry : entries) {
      JMenuItem item = new JMenuItem(entry.label);
      item.addActionListener(event -> entry.action.run());
      popup.add(item);
    } 
    return popup;
  }

  /**
   * Prompts the user to select a file from their system.
   * @param title The title for the FileChooser.
   * @return The file selected by the user.
   */
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
  /**
   * Formats a student name and ID into a list string.
   * @param id The ID of the student.
   * @param name The name of the student.
   * @return The formatted list string.
   */
  @Contract(pure = true)
  private @NotNull String FormatListString(int id, String name) {
    Presence presence = PRESENCE_STATES.get(id);
    Character presenceChar = PRESENCE_CHARS.get(presence);

    return "[" + id + "]: " + name + " (" + presenceChar + ")";
  }

  /**
   * Parses the student ID from the string entry representing a student.
   * @param entry The list string to parse
   * @return The Integer ID of the student.
   */
  @Contract(pure = true)
  private @NotNull Integer ParseIdFromListString(@NotNull String entry) { return Integer.parseInt(entry.split(":")[0].replaceAll("[\\[\\]]","").trim()); }
  
  // Button functions
  /**
   * Creates a new student and places them at the bottom of the register.
   */
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

    STUDENTS.put(nextID, fullName);
    PRESENCE_STATES.put(nextID, Presence.ABSENT);
    UNIQUE_IDS.put(nextID, "N/A");
    STUDENT_LIST_MODEL.addElement(FormatListString(nextID, fullName));
    nextID = RecalculateNextID();

    SetStatus(Status.READY);
  }

  /**
   * Updates the information for the selected student.
   */
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

  /**
   * Deletes the selected student
   * @param OverrideWarning Bypasses the "Are you sure you would like to delete this student?" warning.
   */
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
    UNIQUE_IDS.remove(id);
    STUDENT_LIST_MODEL.remove(selectedIndex);
    nextID = RecalculateNextID();

    SetStatus(Status.READY);
  }

  /**
   * Recalculates and sets "nextID" to the lowest unused ID.
   */
  @Contract(pure = true)
  private @NotNull Integer RecalculateNextID() {
    int i = 1;
    while (STUDENTS.get(i) != null) { i++; }
    return i;
  }

  /**
   * Shows all students and the linked UIDs
   */
  private void ShowStudentUIDs() {
    StringBuilder message = new StringBuilder();
    if (STUDENTS.isEmpty()) { message.append("Register is empty."); }
    else {
      for (int id : STUDENTS.keySet()) {
        String toAppend = "[" + id + "]: " + STUDENTS.get(id) + " | UID: " + UNIQUE_IDS.get(id) + "\n";
        message.append(toAppend);
      }
    }

    JOptionPane.showMessageDialog(
      this,
      message.toString(),
      "Students",
      JOptionPane.INFORMATION_MESSAGE
    );
  }

  /**
   * Prompts the user to set the start time displayed by the program.
   */
  private void SetStartTime() {
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
    if (selectedIndex == -1) return;

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
    if (selectedIndex == -1) return;

    String entry = STUDENT_LIST_MODEL.getElementAt(selectedIndex);
    int id = ParseIdFromListString(entry);

    Presence state = (Presence) SetPresenceBox.getSelectedItem();
    if (state == null) return;

    PRESENCE_STATES.put(id, state);
    STUDENT_LIST_MODEL.set(selectedIndex, FormatListString(id, STUDENTS.get(id)));
    StudentList.revalidate();
    StudentList.repaint();
  }

  /**
   * Toggles the selected student between absent and late/present, depending on StartTime.
   */
  private void ToggleSelectedPresence() {
    int selectedIndex = StudentList.getSelectedIndex();
    if (selectedIndex == -1) return;

    String entry = STUDENT_LIST_MODEL.getElementAt(selectedIndex);
    int id = ParseIdFromListString(entry);

    Presence currentState = PRESENCE_STATES.getOrDefault(id, Presence.ABSENT);
    Presence newState;

    if (currentState == Presence.ABSENT) {newState = GetPresenceFromStartTime();}
    else {newState = Presence.ABSENT;}

    PRESENCE_STATES.put(id, newState);
    SetPresenceBox.setSelectedItem(newState);

    StudentList.revalidate();
    StudentList.repaint();
  }

  /**
   * Sets the presence of a selected student.
   * @param newPresence The presence state to set the student to.
   */
  private void SetPresence(@NotNull Presence newPresence) {SetPresenceBox.setSelectedItem(newPresence);}

  /**
   * Starts listening for cards, runs on the "CardListener" thread.
   */
  private void StartCardListenerAsync() {
    Listening = true;

    CardListenerThread = new Thread(this::ListenForCards);
    CardListenerThread.setDaemon(true);
    CardListenerThread.start();
  }

  /**
   * Stops listening for cards and halts the card listener thread.
   */
  private void StopCardListener() {
    Listening = false;

    if (CardListenerThread != null) CardListenerThread.interrupt();
  }

  /**
   * Listens for cards using the selected card reader, and handles card scans. Should be run asynchronously.
   */
  private void ListenForCards() {
    while (Listening) {
      String UID = NFCHandler.GetUIDFromCard(this, 0, 0);
      SetStatus(Status.READY);
      
      if (mode == TerminalMode.IGNORE) continue;
      if (UID.isEmpty()) continue;

      int selectedIndex = StudentList.getSelectedIndex();
      if (selectedIndex == -1 && mode == TerminalMode.EDIT) continue;

      if (mode == TerminalMode.REGISTER) {
        boolean foundStudent = false;
        
        for (Map.Entry<Integer, String> entry : UNIQUE_IDS.entrySet()) {
          if (!entry.getValue().equals(UID)) continue;

          int id = entry.getKey();
          if (PRESENCE_STATES.get(id) != Presence.ABSENT) break;

          Presence newState = GetPresenceFromStartTime();

          PRESENCE_STATES.put(id, newState);
          SetPresenceBox.setSelectedItem(newState);

          StudentList.revalidate();
          StudentList.repaint();

          foundStudent = true;
          break;
        }
        if (!foundStudent) {JOptionPane.showMessageDialog(this, "No student with UID " + UID + " found.", "Student not found.", JOptionPane.WARNING_MESSAGE);}
      } else {
        String entry = STUDENT_LIST_MODEL.getElementAt(selectedIndex);
        int studentId = ParseIdFromListString(entry);
        String oldUID = UNIQUE_IDS.get(studentId);

        if (oldUID.equals(UID)) {
          JOptionPane.showMessageDialog(
            this,
            "Student [" + studentId + "]: " + STUDENTS.get(studentId) + " has already been assigned UID " + UID + ". UIDs have not been updated.",
            "Duplicate UID",
            JOptionPane.ERROR_MESSAGE
          ); continue;
        }

        if (UNIQUE_IDS.containsValue(UID)) {
          JOptionPane.showMessageDialog(
            this,
            "UID \"" + UID + "\" already in use by [" + studentId + "]: " + STUDENTS.get(studentId) + ". Reset or update their UID and retry.",
            "Duplicate UID",
            JOptionPane.ERROR_MESSAGE
          ); continue;
        }

        int result = JOptionPane.showConfirmDialog(
          this,
          "You are about to update the UID for " + "[" + studentId + "]: " + STUDENTS.get(studentId) + " from \"" + oldUID + "\" to \"" + UID + "\".\n Do you want to continue?",
          "Confirm UID Update",
          JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE
        );
        if (result != JOptionPane.YES_OPTION) continue;

        boolean initialAssign = oldUID.equals("N/A");
        UNIQUE_IDS.put(studentId, UID);
        JOptionPane.showMessageDialog(
          this,
          (initialAssign ? "Set" : "Updated") + " UID for " + STUDENTS.get(studentId) + " to " + UID + (initialAssign ? "" : ("(From " + oldUID + ")")),
          "UID Updated",
          JOptionPane.INFORMATION_MESSAGE
        );
      }
    }
  }

  /**
   * Resets the selected student's UID to "N/A"
   */
  private void ResetSelectedUID() {
    int selectedIndex = StudentList.getSelectedIndex();
    if (selectedIndex == -1) return;
    int studentId = ParseIdFromListString(STUDENT_LIST_MODEL.getElementAt(selectedIndex));

    int confirm = JOptionPane.showConfirmDialog(
      this,
      "You are about to reset the UID for " + STUDENTS.get(studentId) + ". Are you sure you wish to continue?",
      "UID Reset",
      JOptionPane.YES_NO_OPTION
    );
    if (confirm != JOptionPane.YES_OPTION) return;

    UNIQUE_IDS.put(studentId, "N/A");

    JOptionPane.showMessageDialog(
      this,
      "Reset UID for " + STUDENTS.get(studentId),
      "UID Reset",
      JOptionPane.INFORMATION_MESSAGE
    );
  }

  /**
   * Gets the Presence that a student would be based on the current system and start time.
   * @return The Presence of the student.
   */
  private @NotNull Presence GetPresenceFromStartTime() {
    Date now = new Date();

    if (startTime == null || now.before(startTime)) return Presence.PRESENT;
    return Presence.LATE;
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

    String path = selectedFile.getAbsolutePath();
    if (!path.toLowerCase().endsWith(".rsave")) {
      selectedFile = new File(path + ".rsave");
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
        .map(entry -> JSONHandler.CreateStudentJSON(entry.getValue(), entry.getKey().toString(), UNIQUE_IDS.getOrDefault(entry.getKey(), "N/A")))
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

  /**
   * Loads a register from a file. The user is prompted to select a "Register Save" or ".rsave" file from their system.
   */
  private void LoadRegister() {
    File loadedFile = GetFileFromSystem("Load File");
    if (loadedFile == null) {
      SetStatus(Status.READY);
      return;
    }

    try {
      String encodedString = FileHandler.ReadFile(loadedFile);
      if (encodedString.isEmpty()) {
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

        try {
          String UID = obj.getString("UID");
          UNIQUE_IDS.put(id, UID);
        } catch (JSONException err) { UNIQUE_IDS.put(id, "N/A"); }

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

  /**
   * Toggles the fullscreen state of the program.
   */
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

  /**
   * Sets the active terminal to listen to cards from.
   */
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
        NFCHandler.SetActiveTerminal(Option.apply(selected));
        TerminalLabel.setText("Terminal: " + NFCHandler.GetActiveTerminalName());
      }
    }
  }

  /**
   * Sets the terminal mode
   * @param newMode The mode to switch to
   */
  private void SetTerminalMode(@NotNull TerminalMode newMode) { this.mode = newMode; }

  /**
   * Opens the Terminal Tester Utility window
   */
  public void OpenTerminalTester() {
    if (TerminalTesterOpen) {
      int reloadChoice = JOptionPane.showConfirmDialog(
        this,
        "An instance of the Terminal Tester Utility is already open. Would you like to terminate it and open a new one?",
        "Terminal Tester already open",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
      );

      if (reloadChoice != JOptionPane.YES_OPTION) return;
      
      if (terminalTesterWindow != null) {
        terminalTesterWindow.dispose();
        terminalTesterWindow = null;
        TerminalTesterOpen = false;
      } else {
        JOptionPane.showMessageDialog(
          this,
          "Open Terminal Tester instance does not exist or could not be found.",
          "Failed to close Terminal Tester",
          JOptionPane.ERROR_MESSAGE
        ); return; }
    }
    
    IgnoreButton.setSelected(true);
    SetTerminalMode(TerminalMode.IGNORE);
    
    terminalTesterWindow = new TerminalTester(this);
    terminalTesterWindow.setVisible(true);
    TerminalTesterOpen = true;
  }

  /**
   * Configures the card listener to stop on window close or program exit
   */
  private void SetupStopOnClose() {
    this.addComponentListener(new ComponentAdapter() {
      @Override public void componentHidden(ComponentEvent e) { StopCardListener(); }
    });

    this.addWindowListener(new WindowAdapter() {
      @Override public void windowClosing(WindowEvent e) { StopCardListener(); }
      @Override public void windowClosed(WindowEvent e) { StopCardListener(); }
    });
  }

  /**
   * Begins the shutdown sequence.
   */
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
      // Save register and continue exiting
      case JOptionPane.YES_OPTION:
        boolean success = SaveRegister();
        if (success) break;

        // If save failed, abort exit and inform user
        JOptionPane.showMessageDialog(
          this,
          "The current register could not be saved. Ensure the chosen directory exists, or change it, and try again.",
          "Save Failed",
          JOptionPane.ERROR_MESSAGE
        );
        SetStatus(Status.READY);
        return;

      // Do nothing, exit without saving 
      case JOptionPane.NO_OPTION: break;

      // Cancel quitting, resume program
      case JOptionPane.CANCEL_OPTION:
        SetStatus(Status.READY);
        return;
        
      default: throw new UnsupportedOperationException();
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

    // Create popup menus
    JPopupMenu FileMenu = BuildMenu(
      new MenuEntry("Save As (Ctrl + S)", this::SaveRegister),
      new MenuEntry("Load File (Ctrl + O)", this::LoadRegister),
      new MenuEntry("Exit (Escape)", this::Quit)
    );

    JPopupMenu EditMenu = BuildMenu(
      new MenuEntry("Set start time (Ctrl + T)", this::SetStartTime),
      new MenuEntry("Set student Present (Alt + 1)", () -> SetPresence(Presence.PRESENT)),
      new MenuEntry("Set student Late (Alt + 2)", () -> SetPresence(Presence.LATE)),
      new MenuEntry("Set student Absent (Alt + 3)", () -> SetPresence(Presence.ABSENT))
    );

    JPopupMenu ViewMenu = BuildMenu(
      new MenuEntry("Toggle fullscreen (F11)", this::ToggleFullscreen)
    );

    JPopupMenu StudentMenu = BuildMenu(
      new MenuEntry("New student (Ctrl + N)", this::NewStudent),
      new MenuEntry("Edit selected student (Ctrl + E)", this::UpdateStudent),
      new MenuEntry("Delete selected student (Delete)", () -> DeleteStudent(false)),
      new MenuEntry("Toggle Present (Ctrl + Enter)", this::ToggleSelectedPresence),
      new MenuEntry("Show student UIDs (Ctrl + U)", this::ShowStudentUIDs),
      new MenuEntry("Reset selected student's UID (Ctrl + Shift + R)", this::ResetSelectedUID)
    );

    JPopupMenu TerminalMenu = BuildMenu(
      new MenuEntry("Select active terminal (Shift + T)", this::SetActiveTerminal),
      new MenuEntry("Refresh connected terminals (Shift + R)", NFCHandler::RefreshTerminals),
      new MenuEntry("Open Terminal Tester Utility (Ctrl + Shift + T)", this::OpenTerminalTester)
    );

    // Drop Down Buttons
    FileButton = CreateMenuButton("File", FileMenu);
    EditButton = CreateMenuButton("Edit", EditMenu);
    ViewButton = CreateMenuButton("View", ViewMenu);
    StudentButton = CreateMenuButton("Student", StudentMenu);
    TerminalButton = CreateMenuButton("Terminal", TerminalMenu);

    // Status Labels
    StatusLabel = CreateStatusLabel("Status: READY", statusPanel);
    TerminalLabel = CreateStatusLabel("Terminal: " + NFCHandler.GetActiveTerminalName(), statusPanel);
    StartTimeLabel = CreateStatusLabel("Start Time: HH:MM", statusPanel);

    // Add panels to main ContentPane
    ContentPane.setLayout(new BorderLayout());
    ContentPane.add(statusPanel, BorderLayout.SOUTH);
  }

  /**
   * Create and configure a top bar menu button with an associated drop-down.
   * @param text      The text for the JButton.
   * @param popupMenu The JPopupMenu menu to connect the button to.
   * @return          The button, created and formatted.
   */
  private @NotNull JButton CreateMenuButton(@NotNull String text, @NotNull JPopupMenu popupMenu) {
    JButton button = new JButton(text);
    
    button.setFont(new Font("JetBrains Mono", Font.BOLD, 14));
    button.setFocusPainted(false);
    button.setBackground(new Color(240, 240, 240));
    button.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
    button.addActionListener(event -> popupMenu.show(button, 0, button.getHeight()));
    
    return button;
  }

  /**
   * Configure and format a JLabel using the standard Status Label format, and link it to a JPanel.
   * @param text  The text for the label.
   * @param panel The JPanel to link the formatted label to.
   * @return The JLabel, created and formatted.
   */
  private @NotNull JLabel CreateStatusLabel(@NotNull String text, @NotNull JPanel panel) {
    JLabel label = new JLabel(text);
    
    label.setFont(new Font("JetBrains Mono", Font.PLAIN, 20));
    label.setPreferredSize(new Dimension(200, 30));
    label.setForeground(Color.BLACK);
    panel.add(label, BorderLayout.CENTER);
    
    return label;
  }
}