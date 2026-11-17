package hk.edu.polyu.comp.comp2021.crobs.view;

import hk.edu.polyu.comp.comp2021.crobs.controller.CommandProcessor;
import hk.edu.polyu.comp.comp2021.crobs.logging.OperationLogger;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.ScheduleEntry;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The graphical user interface of Crobs (BON1). Every button builds the same text command
 * a user would type in the CLI, logs it and runs it through the shared {@link CommandProcessor},
 * so the GUI and the CLI behave identically and every GUI action appears in both log files.
 * The tables show the model's current rooms, bookings, search results and schedules.
 */
public final class CrobsWindow {

    private static final int WINDOW_WIDTH = 1100;
    private static final int WINDOW_HEIGHT = 760;
    private static final int WIDE_FIELD = 16;
    private static final int ID_FIELD = 10;
    private static final int SHORT_FIELD = 6;
    private static final int CONSOLE_ROWS = 10;
    private static final int CONSOLE_FONT_SIZE = 13;
    private static final double TABLE_SHARE = 0.65;
    private static final String[] ROOM_TYPES = {"lectureTheatre", "computerLab", "seminarRoom", "meetingRoom"};
    private static final String[] ROOM_FIELDS = {"capacity", "roomType", "features"};
    private static final String[] ROLES = {"admin", "regular"};
    private static final String[] ROOM_COLUMNS = {"Room ID", "Room type", "Capacity", "Features"};
    private static final String[] BOOKING_COLUMNS = {"Booking ID", "Room ID", "Date", "Start", "End", "Purpose"};
    private static final String[] SCHEDULE_COLUMNS = {"Booking ID", "Date", "Start", "End", "Owner", "Purpose"};

    private final CROBS crobs;
    private final CommandProcessor processor;
    private final OperationLogger logger;
    private final JFrame frame = new JFrame("Crobs - Campus Room Booking System");
    private final JLabel sessionLabel = new JLabel();
    private final JButton undoButton = new JButton("Undo");
    private final JButton redoButton = new JButton("Redo");
    private final JTextArea console = new JTextArea(CONSOLE_ROWS, 0);
    private final JTextField commandField = new JTextField();

    private final ReadOnlyTableModel roomRows = new ReadOnlyTableModel(ROOM_COLUMNS);
    private final ReadOnlyTableModel bookingRows = new ReadOnlyTableModel(BOOKING_COLUMNS);
    private final ReadOnlyTableModel searchRows = new ReadOnlyTableModel(ROOM_COLUMNS);
    private final ReadOnlyTableModel scheduleRows = new ReadOnlyTableModel(SCHEDULE_COLUMNS);

    private final JTextField loginId = new JTextField("sd00000000", ID_FIELD);
    private final JComboBox<String> newRoomType = new JComboBox<>(ROOM_TYPES);
    private final JTextField newRoomCapacity = new JTextField(SHORT_FIELD);
    private final JTextField newRoomFeatures = new JTextField(WIDE_FIELD);
    private final JTextField roomId = new JTextField(ID_FIELD);
    private final JComboBox<String> roomField = new JComboBox<>(ROOM_FIELDS);
    private final JTextField roomValue = new JTextField(WIDE_FIELD);

    private final JTextField bookingId = new JTextField(SHORT_FIELD);
    private final JTextField bookingRoom = new JTextField(ID_FIELD);
    private final JTextField bookingDate = new JTextField(LocalDate.now().toString(), ID_FIELD);
    private final JTextField bookingStart = new JTextField("09:00", SHORT_FIELD);
    private final JTextField bookingEnd = new JTextField("10:00", SHORT_FIELD);
    private final JTextField bookingPurpose = new JTextField(WIDE_FIELD);

    private final JTextField searchDate = new JTextField(LocalDate.now().toString(), ID_FIELD);
    private final JTextField searchStart = new JTextField("09:00", SHORT_FIELD);
    private final JTextField searchEnd = new JTextField("10:00", SHORT_FIELD);
    private final JTextField searchCapacity = new JTextField("0", SHORT_FIELD);
    private final JTextField searchFeatures = new JTextField("-", WIDE_FIELD);

    private final JTextField scheduleRoom = new JTextField(ID_FIELD);
    private final JTextField scheduleWeek = new JTextField(LocalDate.now().toString(), ID_FIELD);

    private final JTextField newUserName = new JTextField(WIDE_FIELD);
    private final JTextField removeUserId = new JTextField(ID_FIELD);
    private final JTextField roleUserId = new JTextField(ID_FIELD);
    private final JComboBox<String> newRole = new JComboBox<>(ROLES);

    private boolean closed;

    private CrobsWindow(CROBS crobs, OperationLogger logger) {
        this.crobs = Objects.requireNonNull(crobs, "crobs");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.processor = new CommandProcessor(crobs);
        buildFrame();
        console.append("Crobs GUI. Every action is also recorded in the log files.\n");
        console.append("Superadmin user ID: " + crobs.getSuperadminId() + "\n");
        refresh();
    }

    /**
     * Opens the window on the Swing event thread. The window owns the logger and closes it
     * when the user quits or closes the window.
     *
     * @param crobs the model to operate on
     * @param logger the logger that records every command
     */
    public static void open(CROBS crobs, OperationLogger logger) {
        SwingUtilities.invokeLater(() -> new CrobsWindow(crobs, logger).show());
    }

    private void show() {
        frame.setVisible(true);
    }

    private void buildFrame() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                run("quit");
            }
        });
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Rooms", buildRoomsTab());
        tabs.addTab("My bookings", buildBookingsTab());
        tabs.addTab("Search", buildSearchTab());
        tabs.addTab("Schedule", buildScheduleTab());
        tabs.addTab("Users", buildUsersTab());
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tabs, buildConsole());
        split.setResizeWeight(TABLE_SHARE);
        frame.getContentPane().add(buildSessionBar(), BorderLayout.NORTH);
        frame.getContentPane().add(split, BorderLayout.CENTER);
        frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        frame.setLocationRelativeTo(null);
    }

    private JComponent buildSessionBar() {
        sessionLabel.setFont(sessionLabel.getFont().deriveFont(Font.BOLD));
        undoButton.addActionListener(event -> run("undo"));
        redoButton.addActionListener(event -> run("redo"));
        JPanel bar = row(null, new JLabel("User ID"), loginId,
                button("Log in", () -> run("login " + text(loginId))),
                button("Log out", () -> run("logout")), undoButton, redoButton,
                button("Quit", () -> run("quit")));
        bar.add(sessionLabel);
        return bar;
    }

    private JComponent buildRoomsTab() {
        JTable table = table(roomRows);
        table.getSelectionModel().addListSelectionListener(event -> {
            String selected = selectedValue(table, 0);
            if (!selected.isEmpty()) {
                roomId.setText(selected);
                bookingRoom.setText(selected);
                scheduleRoom.setText(selected);
            }
        });
        JPanel forms = new JPanel(new GridLayout(0, 1));
        forms.add(row("Add room (admin)", new JLabel("Type"), newRoomType, new JLabel("Capacity"),
                newRoomCapacity, new JLabel("Features"), newRoomFeatures,
                button("Add room", () -> run("addRoom " + newRoomType.getSelectedItem() + " "
                        + text(newRoomCapacity) + " " + orDash(text(newRoomFeatures))))));
        forms.add(row("Selected room (click a row in the table to select it)", new JLabel("Room ID"), roomId,
                button("Show details", () -> run("listRoom " + text(roomId))),
                button("List all rooms", () -> run("listAllRooms")),
                button("Delete room (admin)", () -> run("deleteRoom " + text(roomId)))));
        forms.add(row("Modify selected room (admin)", new JLabel("Field"), roomField,
                new JLabel("New value"), roomValue,
                button("Modify room", () -> run("modifyRoom " + text(roomId) + " "
                        + roomField.getSelectedItem() + " " + text(roomValue)))));
        return withTable(table, forms);
    }

    private JComponent buildBookingsTab() {
        JTable table = table(bookingRows);
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!selectedValue(table, 0).isEmpty()) {
                bookingId.setText(selectedValue(table, 0));
                bookingRoom.setText(selectedValue(table, 1));
                bookingDate.setText(selectedValue(table, 2));
                bookingStart.setText(selectedValue(table, 3));
                bookingEnd.setText(selectedValue(table, 4));
                bookingPurpose.setText(selectedValue(table, 5));
            }
        });
        JPanel forms = new JPanel(new GridLayout(0, 1));
        forms.add(row("Booking details", new JLabel("Room ID"), bookingRoom, new JLabel("Date"), bookingDate,
                new JLabel("Start"), bookingStart, new JLabel("End"), bookingEnd,
                new JLabel("Purpose"), bookingPurpose));
        forms.add(row("Actions", button("Book", () -> run("book " + bookingDetails())),
                new JLabel("  Booking ID"), bookingId,
                button("Modify booking", () -> run("modifyBooking " + text(bookingId) + " " + bookingDetails())),
                button("Delete booking", () -> run("deleteBooking " + text(bookingId))),
                button("List my bookings", () -> run("listBookings"))));
        return withTable(table, forms);
    }

    private JComponent buildSearchTab() {
        JPanel form = row("Find free rooms", new JLabel("Date"), searchDate, new JLabel("Start"), searchStart,
                new JLabel("End"), searchEnd, new JLabel("Min capacity"), searchCapacity,
                new JLabel("Features"), searchFeatures, button("Search", this::search));
        return withTable(table(searchRows), form);
    }

    private JComponent buildScheduleTab() {
        JPanel form = row("Weekly schedule", new JLabel("Room ID"), scheduleRoom,
                new JLabel("Week start"), scheduleWeek, button("Show schedule", this::schedule));
        return withTable(table(scheduleRows), form);
    }

    private JComponent buildUsersTab() {
        JPanel forms = new JPanel(new GridLayout(0, 1));
        forms.add(row("Add a regular user (admin)", new JLabel("Name"), newUserName,
                button("Add user", () -> run("addUser " + text(newUserName)))));
        forms.add(row("Remove a regular user (admin)", new JLabel("User ID"), removeUserId,
                button("Remove user", () -> run("removeUser " + text(removeUserId)))));
        forms.add(row("Change role (superadmin)", new JLabel("User ID"), roleUserId, newRole,
                button("Change role", () -> run("changeRole " + text(roleUserId) + " "
                        + newRole.getSelectedItem()))));
        JPanel tab = new JPanel(new BorderLayout());
        tab.add(forms, BorderLayout.NORTH);
        return tab;
    }

    private JComponent buildConsole() {
        console.setEditable(false);
        console.setFont(new Font(Font.MONOSPACED, Font.PLAIN, CONSOLE_FONT_SIZE));
        commandField.addActionListener(event -> runTypedCommand());
        JPanel input = new JPanel(new BorderLayout());
        input.add(new JLabel(" crobs> "), BorderLayout.WEST);
        input.add(commandField, BorderLayout.CENTER);
        input.add(button("Run", this::runTypedCommand), BorderLayout.EAST);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Output (you can also type any CLI command)"));
        panel.add(new JScrollPane(console), BorderLayout.CENTER);
        panel.add(input, BorderLayout.SOUTH);
        return panel;
    }

    private void runTypedCommand() {
        String command = commandField.getText();
        commandField.setText("");
        run(command);
    }

    private void search() {
        run("search " + text(searchDate) + " " + text(searchStart) + " " + text(searchEnd) + " "
                + text(searchCapacity) + " " + orDash(text(searchFeatures)));
        searchRows.setRowCount(0);
        try {
            List<Room> rooms = crobs.search(LocalDate.parse(text(searchDate)), LocalTime.parse(text(searchStart)),
                    LocalTime.parse(text(searchEnd)), Integer.parseInt(text(searchCapacity)),
                    orDash(text(searchFeatures)));
            fillRooms(searchRows, rooms);
        } catch (DateTimeException | IllegalArgumentException | IllegalStateException invalid) {
            fillRooms(searchRows, Collections.<Room>emptyList());
        }
    }

    private void schedule() {
        run("schedule " + text(scheduleRoom) + " " + text(scheduleWeek));
        scheduleRows.setRowCount(0);
        try {
            for (ScheduleEntry entry : crobs.schedule(text(scheduleRoom), LocalDate.parse(text(scheduleWeek)))) {
                String owner = entry.getOwnerUserId() == null ? "hidden" : entry.getOwnerUserId();
                scheduleRows.addRow(new Object[]{entry.getBookingId(), entry.getDate(), entry.getStartTime(),
                        entry.getEndTime(), owner, entry.getPurpose()});
            }
        } catch (DateTimeException | IllegalArgumentException | IllegalStateException invalid) {
            scheduleRows.setRowCount(0);
        }
    }

    /** Logs a command, runs it, shows its output and refreshes the tables. */
    private void run(String command) {
        if (closed) {
            return;
        }
        try {
            logger.log(command);
        } catch (IOException | IllegalArgumentException logFailure) {
            JOptionPane.showMessageDialog(frame, "Cannot write the log files: " + logFailure.getMessage(),
                    "Crobs", JOptionPane.ERROR_MESSAGE);
            close();
            return;
        }

        StringWriter buffer = new StringWriter();
        PrintWriter output = new PrintWriter(buffer);
        String previousUserId = crobs.getCurrentUser().map(User::getUserId).orElse(null);

        boolean keepRunning = processor.execute(command, output);

        String currentUserId = crobs.getCurrentUser().map(User::getUserId).orElse(null);
        if (!java.util.Objects.equals(previousUserId, currentUserId)) {
            console.setText("");
            scheduleRows.setRowCount(0);
            searchRows.setRowCount(0);
        }

        console.append("crobs> " + command + "\n" + buffer);
        console.setCaretPosition(console.getDocument().getLength());
        refresh();
        if (!keepRunning) {
            close();
        }
    }

    private void refresh() {
        Optional<User> user = crobs.getCurrentUser();
        sessionLabel.setText(user.map(current -> "   Logged in as " + current.getUserId() + " ("
                + current.getRole() + ")").orElse("   Not logged in"));
        undoButton.setEnabled(crobs.canUndo());
        redoButton.setEnabled(crobs.canRedo());
        if (user.isPresent()) {
            fillRooms(roomRows, crobs.listAllRooms());
            fillBookings(crobs.listBookings());
        } else {
            fillRooms(roomRows, Collections.<Room>emptyList());
            fillBookings(Collections.<Booking>emptyList());
        }
    }

    private static void fillRooms(DefaultTableModel rows, List<Room> rooms) {
        rows.setRowCount(0);
        for (Room room : rooms) {
            rows.addRow(new Object[]{room.getRoomID(), room.getRoomType(), room.getCapacity(),
                    String.join(",", room.getFeatures())});
        }
    }

    private void fillBookings(List<Booking> bookings) {
        bookingRows.setRowCount(0);
        for (Booking booking : bookings) {
            bookingRows.addRow(new Object[]{booking.getBookingId(), booking.getRoomId(), booking.getDate(),
                    booking.getStartTime(), booking.getEndTime(), booking.getPurpose()});
        }
    }

    private void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            logger.close();
        } catch (IOException closeFailure) {
            JOptionPane.showMessageDialog(frame, "Cannot close the log files: " + closeFailure.getMessage(),
                    "Crobs", JOptionPane.ERROR_MESSAGE);
        }
        frame.dispose();
    }

    private String bookingDetails() {
        return text(bookingRoom) + " " + text(bookingDate) + " " + text(bookingStart) + " "
                + text(bookingEnd) + " " + text(bookingPurpose);
    }

    private static String text(JTextField field) {
        return field.getText().trim();
    }

    private static String orDash(String features) {
        return features.isEmpty() ? "-" : features;
    }

    private static String selectedValue(JTable table, int column) {
        int row = table.getSelectedRow();
        if (row < 0) {
            return "";
        }
        return String.valueOf(table.getValueAt(row, column));
    }

    private static JTable table(DefaultTableModel rows) {
        JTable table = new JTable(rows);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        return table;
    }

    private static JComponent withTable(JTable table, JComponent forms) {
        JPanel tab = new JPanel(new BorderLayout());
        tab.add(new JScrollPane(table), BorderLayout.CENTER);
        tab.add(forms, BorderLayout.SOUTH);
        return tab;
    }

    private static JButton button(String label, Runnable action) {
        JButton button = new JButton(label);
        button.addActionListener(event -> action.run());
        return button;
    }

    private static JPanel row(String title, JComponent... parts) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        if (title != null) {
            panel.setBorder(BorderFactory.createTitledBorder(title));
        }
        for (JComponent part : parts) {
            panel.add(part);
        }
        return panel;
    }

    /** A table model whose cells cannot be edited by the user. */
    private static final class ReadOnlyTableModel extends DefaultTableModel {

        private static final long serialVersionUID = 1L;

        /**
         * Creates an empty table with the given columns.
         *
         * @param columns the column titles
         */
        ReadOnlyTableModel(String[] columns) {
            super(columns, 0);
        }

        /**
         * Prevents editing in the table; changes go through commands instead.
         *
         * @param row the row index
         * @param column the column index
         * @return always false
         */
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
