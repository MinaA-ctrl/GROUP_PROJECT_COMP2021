package hk.edu.polyu.comp.comp2021.crobs.view.portal;

import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** The start page: a welcome message, key figures, quick actions and upcoming bookings. */
final class DashboardPage extends PortalPage {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

    private final JLabel welcome = PortalTheme.label("", PortalTheme.headingFont(), PortalTheme.ink());
    private final JLabel roomCount = PortalTheme.label("0", PortalTheme.statFont(), PortalTheme.ink());
    private final JLabel bookingCount = PortalTheme.label("0", PortalTheme.statFont(), PortalTheme.ink());
    private final JLabel nextBooking = PortalTheme.label("-", PortalTheme.headingFont(), PortalTheme.ink());
    private final JLabel nextBookingRoom = PortalTheme.label(" ", PortalTheme.bodyFont(), PortalTheme.slate());
    private final PortalTheme.ReadOnlyTableModel upcoming =
            new PortalTheme.ReadOnlyTableModel("Date", "Time", "Room", "Purpose");

    /**
     * Creates the dashboard.
     *
     * @param session the portal session
     * @param openFind opens the Find a room page
     * @param openSchedule opens the Schedule page
     * @param openBookings opens the My bookings page
     * @param newBooking opens the booking dialog
     */
    DashboardPage(PortalSession session, Runnable openFind, Runnable openSchedule, Runnable openBookings,
                  Runnable newBooking) {
        super(session, "Dashboard", "An overview of campus rooms and your bookings.");
        JPanel stats = new JPanel(new GridLayout(1, 3, PortalTheme.gap(), 0));
        stats.setOpaque(false);
        stats.add(stat("Rooms on campus", roomCount, new JLabel(" ")));
        stats.add(stat("My upcoming bookings", bookingCount, new JLabel(" ")));
        stats.add(stat("Next booking", nextBooking, nextBookingRoom));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, PortalTheme.gap(), 0));
        actions.setOpaque(false);
        actions.add(PortalTheme.primary("Book a room", newBooking));
        actions.add(PortalTheme.secondary("Find a free room", openFind));
        actions.add(PortalTheme.secondary("Room schedules", openSchedule));
        actions.add(PortalTheme.secondary("My bookings", openBookings));

        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        welcome.setAlignmentX(LEFT_ALIGNMENT);
        stats.setAlignmentX(LEFT_ALIGNMENT);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        top.add(welcome);
        top.add(spacer());
        top.add(stats);
        top.add(spacer());
        top.add(actions);
        top.add(spacer());

        JPanel list = new JPanel(new BorderLayout(0, PortalTheme.gap() / 2));
        list.add(PortalTheme.label("Your upcoming bookings", PortalTheme.headingFont(), PortalTheme.ink()),
                BorderLayout.NORTH);
        list.add(new JScrollPane(PortalTheme.table(upcoming)), BorderLayout.CENTER);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(top, BorderLayout.NORTH);
        content.add(PortalTheme.card(list), BorderLayout.CENTER);
        setContent(content);
    }

    /** Updates the welcome message, the figures and the upcoming bookings. */
    @Override
    void refresh() {
        upcoming.setRowCount(0);
        if (!currentUser().isPresent()) {
            return;
        }
        User user = currentUser().get();
        welcome.setText("Welcome, " + user.getName() + " (" + user.getRole() + ")  ·  "
                + LocalDate.now().format(LONG_DATE));
        roomCount.setText(String.valueOf(model().listAllRooms().size()));
        List<Booking> future = upcomingBookings();
        bookingCount.setText(String.valueOf(future.size()));
        if (future.isEmpty()) {
            nextBooking.setText("None");
            nextBookingRoom.setText("Book a room to get started");
        } else {
            Booking next = future.get(0);
            nextBooking.setText(next.getDate().format(SHORT_DATE) + ", " + next.getStartTime());
            nextBookingRoom.setText(next.getRoomId() + " · " + next.getPurpose());
        }
        for (Booking booking : future) {
            upcoming.addRow(new Object[]{booking.getDate().format(SHORT_DATE),
                    booking.getStartTime() + " – " + booking.getEndTime(), booking.getRoomId(), booking.getPurpose()});
        }
    }

    private List<Booking> upcomingBookings() {
        LocalDateTime now = LocalDateTime.now();
        List<Booking> result = new ArrayList<>();
        for (Booking booking : model().listBookings()) {
            if (!booking.getDate().atTime(booking.getEndTime()).isBefore(now)) {
                result.add(booking);
            }
        }
        return Collections.unmodifiableList(result);
    }

    private static JPanel stat(String title, JLabel value, JLabel detail) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(PortalTheme.label(title.toUpperCase(Locale.ENGLISH), PortalTheme.bodyFont(), PortalTheme.slate()));
        box.add(value);
        box.add(detail);
        JPanel card = PortalTheme.card(box);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, PortalTheme.moss()), card.getBorder()));
        return card;
    }

    private static JPanel spacer() {
        JPanel gap = new JPanel();
        gap.setOpaque(false);
        gap.setBorder(BorderFactory.createEmptyBorder(PortalTheme.gap() / 2, 0, PortalTheme.gap() / 2, 0));
        return gap;
    }
}
