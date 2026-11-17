package hk.edu.polyu.comp.comp2021.crobs.view.portal;

import hk.edu.polyu.comp.comp2021.crobs.model.Room;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.function.Consumer;

/** Finds rooms that are free at a given time, are large enough and have the required features. */
final class FindRoomPage extends PortalPage {

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_START_HOUR = 9;
    private static final int MAX_CAPACITY = 1000;

    private final JSpinner date = PortalForms.datePicker(LocalDate.now().plusDays(1));
    private final JComboBox<String> start = PortalForms.timePicker(LocalTime.of(DEFAULT_START_HOUR, 0));
    private final JComboBox<String> end = PortalForms.timePicker(LocalTime.of(DEFAULT_START_HOUR + 1, 0));
    private final JSpinner capacity = new JSpinner(new SpinnerNumberModel(0, 0, MAX_CAPACITY, 1));
    private final JTextField features = PortalForms.textField("");
    private final PortalTheme.ReadOnlyTableModel rows =
            new PortalTheme.ReadOnlyTableModel("Room ID", "Type", "Capacity", "Features");
    private final JTable table = PortalTheme.table(rows);
    private final transient Consumer<PortalForms.BookingDraft> bookRoom;

    /**
     * Creates the search page.
     *
     * @param session the portal session
     * @param bookRoom opens the booking dialog with the given values
     */
    FindRoomPage(PortalSession session, Consumer<PortalForms.BookingDraft> bookRoom) {
        super(session, "Find a room", "Rooms that are free at the chosen time, large enough and with all the features you need.");
        this.bookRoom = bookRoom;
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, PortalTheme.gap() / 2, 0));
        filters.add(PortalForms.form("Date", date, "From", start));
        filters.add(PortalForms.form("Until", end, "Min. seats", capacity));
        filters.add(PortalForms.form("Features", features, "", PortalTheme.label("e.g. projector,whiteboard",
                PortalTheme.bodyFont(), PortalTheme.slate())));
        filters.add(PortalTheme.primary("Search", this::search));
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        actions.setOpaque(false);
        actions.add(PortalTheme.primary("Book selected room", this::bookSelected));
        JPanel results = new JPanel(new BorderLayout(0, PortalTheme.gap()));
        results.setOpaque(false);
        results.add(PortalTheme.card(new JScrollPane(table)), BorderLayout.CENTER);
        results.add(actions, BorderLayout.SOUTH);
        JPanel content = new JPanel(new BorderLayout(0, PortalTheme.gap()));
        content.setOpaque(false);
        content.add(PortalTheme.card(filters), BorderLayout.NORTH);
        content.add(results, BorderLayout.CENTER);
        setContent(content);
    }

    /** Clears the results when nobody is logged in; otherwise the last results stay visible. */
    @Override
    void refresh() {
        if (!currentUser().isPresent()) {
            rows.setRowCount(0);
        }
    }

    private void search() {
        String featureList = features.getText().trim().replace(" ", "");
        PortalSession.Result result = run("search " + PortalForms.dateOf(date) + " " + start.getSelectedItem() + " "
                + end.getSelectedItem() + " " + capacity.getValue() + " " + (featureList.isEmpty() ? "-" : featureList));
        rows.setRowCount(0);
        if (!result.isSuccess()) {
            return;
        }
        try {
            for (Room room : model().search(PortalForms.dateOf(date), LocalTime.parse(String.valueOf(start.getSelectedItem())),
                    LocalTime.parse(String.valueOf(end.getSelectedItem())), (Integer) capacity.getValue(),
                    featureList.isEmpty() ? "-" : featureList)) {
                rows.addRow(new Object[]{room.getRoomID(), room.getRoomType(), room.getCapacity(),
                        room.getFeatures().isEmpty() ? "–" : String.join(", ", room.getFeatures())});
            }
        } catch (DateTimeException | IllegalArgumentException | IllegalStateException invalid) {
            rows.setRowCount(0);
        }
    }

    private void bookSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Search first, then select a room in the results.");
            return;
        }
        bookRoom.accept(new PortalForms.BookingDraft("", String.valueOf(rows.getValueAt(row, 0)),
                PortalForms.dateOf(date), LocalTime.parse(String.valueOf(start.getSelectedItem())),
                LocalTime.parse(String.valueOf(end.getSelectedItem())), ""));
    }
}
