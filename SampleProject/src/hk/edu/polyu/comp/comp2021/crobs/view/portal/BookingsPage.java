package hk.edu.polyu.comp.comp2021.crobs.view.portal;

import hk.edu.polyu.comp.comp2021.crobs.model.Booking;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/** Lists the user's bookings and lets the user create, change and cancel them. */
final class BookingsPage extends PortalPage {

    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_START_HOUR = 9;

    private final PortalTheme.ReadOnlyTableModel rows =
            new PortalTheme.ReadOnlyTableModel("Booking", "Room", "Date", "Start", "End", "Purpose");
    private final JTable table = PortalTheme.table(rows);

    /**
     * Creates the bookings page.
     *
     * @param session the portal session
     */
    BookingsPage(PortalSession session) {
        super(session, "My bookings", "Your bookings in date and time order.");
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, PortalTheme.gap() / 2, 0));
        toolbar.setOpaque(false);
        toolbar.add(PortalTheme.primary("New booking", () -> newBooking("")));
        toolbar.add(PortalTheme.secondary("Edit booking", this::editBooking));
        toolbar.add(PortalTheme.danger("Cancel booking", this::cancelBooking));
        toolbar.add(PortalTheme.secondary("Refresh list", () -> run("listBookings")));
        JPanel content = new JPanel(new BorderLayout(0, PortalTheme.gap()));
        content.setOpaque(false);
        content.add(toolbar, BorderLayout.NORTH);
        content.add(PortalTheme.card(new JScrollPane(table)), BorderLayout.CENTER);
        setContent(content);
    }

    /** Reloads the user's bookings. */
    @Override
    void refresh() {
        rows.setRowCount(0);
        if (!currentUser().isPresent()) {
            return;
        }
        for (Booking booking : model().listBookings()) {
            rows.addRow(new Object[]{booking.getBookingId(), booking.getRoomId(), booking.getDate(),
                    booking.getStartTime(), booking.getEndTime(), booking.getPurpose()});
        }
    }

    /**
     * Opens the booking dialog for a new booking.
     *
     * @param roomId the room to preselect, or an empty string
     */
    void newBooking(String roomId) {
        LocalTime start = LocalTime.of(DEFAULT_START_HOUR, 0);
        book(new PortalForms.BookingDraft("", roomId, LocalDate.now().plusDays(1), start, start.plusHours(1), ""));
    }

    /**
     * Opens the booking dialog with the given values and runs the resulting command.
     *
     * @param draft the values shown at first
     */
    void book(PortalForms.BookingDraft draft) {
        Optional<String> command = PortalForms.bookingDialog(this, model().listAllRooms(), draft);
        command.ifPresent(this::run);
    }

    private void editBooking() {
        Optional<Booking> selected = selected();
        selected.ifPresent(b -> book(new PortalForms.BookingDraft(b.getBookingId(), b.getRoomId(), b.getDate(),
                b.getStartTime(), b.getEndTime(), b.getPurpose())));
    }

    private void cancelBooking() {
        Optional<Booking> selected = selected();
        if (selected.isPresent() && PortalForms.confirm(this, "Cancel booking " + selected.get().getBookingId()
                + " (" + selected.get().getRoomId() + ", " + selected.get().getDate() + ")?")) {
            run("deleteBooking " + selected.get().getBookingId());
        }
    }

    private Optional<Booking> selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a booking in the table first.");
            return Optional.empty();
        }
        String id = String.valueOf(rows.getValueAt(row, 0));
        return model().listBookings().stream().filter(b -> b.getBookingId().equals(id)).findFirst();
    }
}
