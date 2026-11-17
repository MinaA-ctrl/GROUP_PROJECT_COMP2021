package hk.edu.polyu.comp.comp2021.crobs.view.portal;

import hk.edu.polyu.comp.comp2021.crobs.model.Room;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/** Shows the weekly schedule of one room as a calendar grid. */
final class SchedulePage extends PortalPage {

    private static final long serialVersionUID = 1L;
    private static final int DAYS_PER_WEEK = 7;

    private final JComboBox<String> room = new JComboBox<>();
    private final JPanel weekHolder = new JPanel(new BorderLayout());
    private JSpinner week = PortalForms.datePicker(LocalDate.now().with(DayOfWeek.MONDAY));
    private final WeekCalendar calendar = new WeekCalendar();
    private String shownRoom = "";
    private LocalDate shownWeek = LocalDate.now();

    /**
     * Creates the schedule page.
     *
     * @param session the portal session
     */
    SchedulePage(PortalSession session) {
        super(session, "Room schedule", "The bookings of a room in the seven days from the chosen start date.");
        weekHolder.setOpaque(false);
        weekHolder.add(week, BorderLayout.CENTER);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, PortalTheme.gap() / 2, 0));
        toolbar.setOpaque(false);
        toolbar.add(PortalForms.form("Room", room));
        toolbar.add(PortalForms.form("Week starting", weekHolder));
        toolbar.add(PortalTheme.secondary("◀ Previous week", () -> moveWeek(-DAYS_PER_WEEK)));
        toolbar.add(PortalTheme.secondary("Next week ▶", () -> moveWeek(DAYS_PER_WEEK)));
        toolbar.add(PortalTheme.primary("Show schedule", this::showSchedule));
        JPanel content = new JPanel(new BorderLayout(0, PortalTheme.gap()));
        content.setOpaque(false);
        content.add(toolbar, BorderLayout.NORTH);
        JScrollPane scroller = new JScrollPane(calendar);
        scroller.getViewport().setBackground(PortalTheme.paper());
        scroller.setBorder(BorderFactory.createEmptyBorder());
        content.add(PortalTheme.card(scroller), BorderLayout.CENTER);
        setContent(content);
    }

    /** Updates the room list and redraws the shown week from the model. */
    @Override
    void refresh() {
        if (!currentUser().isPresent()) {
            room.removeAllItems();
            calendar.clear();
            shownRoom = "";
            return;
        }
        String selected = PortalForms.roomIdOf(room);
        List<Room> rooms = model().listAllRooms();
        room.removeAllItems();
        JComboBox<String> fresh = PortalForms.roomPicker(rooms, selected);
        for (int i = 0; i < fresh.getItemCount(); i++) {
            room.addItem(fresh.getItemAt(i));
        }
        room.setSelectedItem(fresh.getSelectedItem());
        if (!shownRoom.isEmpty()) {
            if (containsRoom(rooms, shownRoom)) {
                calendar.show(shownWeek, model().schedule(shownRoom, shownWeek));
            } else {
                calendar.clear();
            }
        }
    }

    /**
     * Shows the schedule of a room for the week containing a date.
     *
     * @param roomId the room
     * @param date a date in the week to show
     */
    void showRoom(String roomId, LocalDate date) {
        for (int i = 0; i < room.getItemCount(); i++) {
            if (room.getItemAt(i).startsWith(roomId + " ")) {
                room.setSelectedIndex(i);
            }
        }
        setWeek(date.with(DayOfWeek.MONDAY));
        showSchedule();
    }

    private void moveWeek(int days) {
        setWeek(PortalForms.dateOf(week).plusDays(days));
        if (!PortalForms.roomIdOf(room).isEmpty()) {
            showSchedule();
        }
    }

    private void setWeek(LocalDate start) {
        weekHolder.remove(week);
        week = PortalForms.datePicker(start);
        weekHolder.add(week, BorderLayout.CENTER);
        weekHolder.revalidate();
    }

    private void showSchedule() {
        String roomId = PortalForms.roomIdOf(room);
        LocalDate start = PortalForms.dateOf(week);
        PortalSession.Result result = run("schedule " + roomId + " " + start);
        if (result.isSuccess()) {
            shownRoom = roomId;
            shownWeek = start;
            calendar.show(start, model().schedule(roomId, start));
        } else {
            shownRoom = "";
            calendar.clear();
        }
    }

    private static boolean containsRoom(List<Room> rooms, String roomId) {
        for (Room candidate : rooms) {
            if (candidate.getRoomID().equals(roomId)) {
                return true;
            }
        }
        return false;
    }
}
