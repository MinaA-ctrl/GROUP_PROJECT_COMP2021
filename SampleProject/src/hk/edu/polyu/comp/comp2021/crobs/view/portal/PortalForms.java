package hk.edu.polyu.comp.comp2021.crobs.view.portal;

import hk.edu.polyu.comp.comp2021.crobs.model.Room;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerListModel;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Input widgets and dialogs of the portal. Dates and times are chosen from lists, so the
 * commands they produce are always well formed; the model still checks everything else.
 */
final class PortalForms {

    private static final int PAST_DAYS = 30;
    private static final int FUTURE_DAYS = 365;
    private static final int FIRST_HOUR = 7;
    private static final int LAST_HOUR = 22;
    private static final int STEP_MINUTES = 30;
    private static final int TEXT_COLUMNS = 18;

    private PortalForms() {
    }

    /**
     * Creates a date picker showing dates as YYYY-MM-DD.
     *
     * @param value the date selected at first
     * @return the picker
     */
    static JSpinner datePicker(LocalDate value) {
        LocalDate today = LocalDate.now();
        LocalDate first = (value.isBefore(today) ? value : today).minusDays(PAST_DAYS);
        LocalDate last = (value.isAfter(today) ? value : today).plusDays(FUTURE_DAYS);
        List<String> dates = new ArrayList<>();
        for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1)) {
            dates.add(day.toString());
        }
        JSpinner spinner = new JSpinner(new SpinnerListModel(dates));
        spinner.setValue(value.toString());
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setEditable(false);
        return spinner;
    }

    /**
     * Reads the date of a date picker.
     *
     * @param picker a picker made by {@link #datePicker(LocalDate)}
     * @return the selected date
     */
    static LocalDate dateOf(JSpinner picker) {
        return LocalDate.parse(String.valueOf(picker.getValue()));
    }

    /**
     * Creates a time picker with half-hour steps during opening hours.
     *
     * @param value the time selected at first
     * @return the picker
     */
    static JComboBox<String> timePicker(LocalTime value) {
        JComboBox<String> box = new JComboBox<>();
        for (LocalTime time = LocalTime.of(FIRST_HOUR, 0); !time.isAfter(LocalTime.of(LAST_HOUR, 0));
             time = time.plusMinutes(STEP_MINUTES)) {
            box.addItem(time.toString());
        }
        box.setSelectedItem(value.toString());
        if (box.getSelectedIndex() < 0) {
            box.addItem(value.toString());
            box.setSelectedItem(value.toString());
        }
        return box;
    }

    /**
     * Creates a room picker that shows type and capacity next to each room ID.
     *
     * @param rooms the rooms to choose from
     * @param selectedId the room selected at first, or an empty string
     * @return the picker
     */
    static JComboBox<String> roomPicker(List<Room> rooms, String selectedId) {
        JComboBox<String> box = new JComboBox<>();
        for (Room room : rooms) {
            String item = describe(room);
            box.addItem(item);
            if (room.getRoomID().equals(selectedId)) {
                box.setSelectedItem(item);
            }
        }
        return box;
    }

    /**
     * Describes a room for pickers and dialogs.
     *
     * @param room the room
     * @return e.g. "mr00000000 · meetingRoom · 8 seats"
     */
    static String describe(Room room) {
        return room.getRoomID() + " · " + room.getRoomType() + " · " + room.getCapacity() + " seats";
    }

    /**
     * Returns the room ID selected in a room picker.
     *
     * @param picker a picker made by {@link #roomPicker(List, String)}
     * @return the room ID, or an empty string if there are no rooms
     */
    static String roomIdOf(JComboBox<String> picker) {
        Object selected = picker.getSelectedItem();
        return selected == null ? "" : selected.toString().split(" ")[0];
    }

    /**
     * Lays out labels and fields in two columns.
     *
     * @param labelsAndFields alternating label texts and components
     * @return the form panel
     */
    static JPanel form(Object... labelsAndFields) {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        for (int i = 0; i + 1 < labelsAndFields.length; i += 2) {
            c.gridy = i / 2;
            c.gridx = 0;
            c.fill = GridBagConstraints.NONE;
            JLabel label = PortalTheme.label(String.valueOf(labelsAndFields[i]), PortalTheme.bodyFont(),
                    PortalTheme.slate());
            form.add(label, c);
            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            form.add((Component) labelsAndFields[i + 1], c);
        }
        return form;
    }

    /**
     * Creates a text field of the usual width.
     *
     * @param text the initial text
     * @return the field
     */
    static JTextField textField(String text) {
        JTextField field = new JTextField(text, TEXT_COLUMNS);
        field.setFont(PortalTheme.bodyFont());
        return field;
    }

    /**
     * Shows a form in an OK/Cancel dialog.
     *
     * @param parent the component the dialog belongs to
     * @param title the dialog title
     * @param form the form
     * @return true if the user clicked OK
     */
    static boolean showForm(Component parent, String title, JComponent form) {
        return JOptionPane.showConfirmDialog(parent, form, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE, new UniCrest(PortalTheme.pageGap() * 2)) == JOptionPane.OK_OPTION;
    }

    /**
     * Asks a yes/no question.
     *
     * @param parent the component the dialog belongs to
     * @param message the question
     * @return true if the user answered yes
     */
    static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /**
     * Shows the booking dialog and builds the matching book or modifyBooking command.
     *
     * @param parent the component the dialog belongs to
     * @param rooms the rooms to choose from
     * @param draft the values shown at first
     * @return the command, or an empty optional if the user cancelled
     */
    static Optional<String> bookingDialog(Component parent, List<Room> rooms, BookingDraft draft) {
        if (rooms.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "There are no rooms to book yet.");
            return Optional.empty();
        }
        JComboBox<String> room = roomPicker(rooms, draft.getRoomId());
        JSpinner date = datePicker(draft.getDate());
        JComboBox<String> start = timePicker(draft.getStart());
        JComboBox<String> end = timePicker(draft.getEnd());
        JTextField purpose = textField(draft.getPurpose());
        String title = draft.getBookingId().isEmpty() ? "New booking" : "Edit booking " + draft.getBookingId();
        if (!showForm(parent, title, form("Room", room, "Date", date, "Start", start, "End", end,
                "Purpose", purpose))) {
            return Optional.empty();
        }
        String details = roomIdOf(room) + " " + dateOf(date) + " " + start.getSelectedItem() + " "
                + end.getSelectedItem() + " " + purpose.getText().trim();
        return Optional.of(draft.getBookingId().isEmpty() ? "book " + details
                : "modifyBooking " + draft.getBookingId() + " " + details);
    }

    /** The values shown when the booking dialog opens. */
    static final class BookingDraft {

        private final String bookingId;
        private final String roomId;
        private final LocalDate date;
        private final LocalTime start;
        private final LocalTime end;
        private final String purpose;

        /**
         * Creates a draft.
         *
         * @param bookingId the booking being edited, or an empty string for a new booking
         * @param roomId the room, or an empty string
         * @param date the date
         * @param start the start time
         * @param end the end time
         * @param purpose the purpose
         */
        BookingDraft(String bookingId, String roomId, LocalDate date, LocalTime start, LocalTime end,
                     String purpose) {
            this.bookingId = bookingId;
            this.roomId = roomId;
            this.date = date;
            this.start = start;
            this.end = end;
            this.purpose = purpose;
        }

        /**
         * Returns the booking being edited.
         *
         * @return the booking ID, or an empty string for a new booking
         */
        String getBookingId() {
            return bookingId;
        }

        /**
         * Returns the room.
         *
         * @return the room ID, or an empty string
         */
        String getRoomId() {
            return roomId;
        }

        /**
         * Returns the date.
         *
         * @return the date
         */
        LocalDate getDate() {
            return date;
        }

        /**
         * Returns the start time.
         *
         * @return the start time
         */
        LocalTime getStart() {
            return start;
        }

        /**
         * Returns the end time.
         *
         * @return the end time
         */
        LocalTime getEnd() {
            return end;
        }

        /**
         * Returns the purpose.
         *
         * @return the purpose
         */
        String getPurpose() {
            return purpose;
        }
    }
}
