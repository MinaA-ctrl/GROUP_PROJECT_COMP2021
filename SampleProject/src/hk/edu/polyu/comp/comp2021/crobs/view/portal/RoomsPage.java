package hk.edu.polyu.comp.comp2021.crobs.view.portal;

import hk.edu.polyu.comp.comp2021.crobs.model.Room;

import javax.swing.JButton;
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
import java.util.ArrayList;
import java.util.List;

/** Lists all rooms; admins can also add, edit and delete rooms here. */
final class RoomsPage extends PortalPage {

    private static final long serialVersionUID = 1L;
    private static final String ALL_TYPES = "All room types";
    private static final String[] ROOM_TYPES = {"lectureTheatre", "computerLab", "seminarRoom", "meetingRoom"};
    private static final int MAX_CAPACITY = 1000;

    private final PortalTheme.ReadOnlyTableModel rows =
            new PortalTheme.ReadOnlyTableModel("Room ID", "Type", "Capacity", "Features");
    private final JTable table = PortalTheme.table(rows);
    private final JComboBox<String> typeFilter = new JComboBox<>();
    private final List<JButton> adminButtons = new ArrayList<>();

    /**
     * Creates the rooms page.
     *
     * @param session the portal session
     */
    RoomsPage(PortalSession session) {
        super(session, "Rooms", "All bookable rooms on campus, sorted by room ID.");
        typeFilter.addItem(ALL_TYPES);
        for (String type : ROOM_TYPES) {
            typeFilter.addItem(type);
        }
        typeFilter.addActionListener(event -> refresh());
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, PortalTheme.gap() / 2, 0));
        toolbar.setOpaque(false);
        toolbar.add(typeFilter);
        toolbar.add(PortalTheme.secondary("Room details", this::showDetails));
        adminButtons.add(PortalTheme.primary("Add room", this::addRoom));
        adminButtons.add(PortalTheme.secondary("Edit room", this::editRoom));
        adminButtons.add(PortalTheme.danger("Delete room", this::deleteRoom));
        for (JButton button : adminButtons) {
            toolbar.add(button);
        }
        JPanel content = new JPanel(new BorderLayout(0, PortalTheme.gap()));
        content.setOpaque(false);
        content.add(toolbar, BorderLayout.NORTH);
        content.add(PortalTheme.card(new JScrollPane(table)), BorderLayout.CENTER);
        setContent(content);
    }

    /** Reloads the room table and shows admin buttons only to admins. */
    @Override
    void refresh() {
        rows.setRowCount(0);
        for (JButton button : adminButtons) {
            button.setVisible(isAdmin());
        }
        if (!currentUser().isPresent()) {
            return;
        }
        Object filter = typeFilter.getSelectedItem();
        for (Room room : model().listAllRooms()) {
            if (ALL_TYPES.equals(filter) || room.getRoomType().toString().equals(filter)) {
                rows.addRow(new Object[]{room.getRoomID(), room.getRoomType(), room.getCapacity(),
                        room.getFeatures().isEmpty() ? "–" : String.join(", ", room.getFeatures())});
            }
        }
    }

    private String selectedRoomId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a room in the table first.");
            return "";
        }
        return String.valueOf(rows.getValueAt(row, 0));
    }

    private void showDetails() {
        String id = selectedRoomId();
        if (!id.isEmpty()) {
            PortalSession.Result result = run("listRoom " + id);
            JOptionPane.showMessageDialog(this, result.getOutput().replace('\t', ' '), "Room " + id,
                    JOptionPane.INFORMATION_MESSAGE, new UniCrest(PortalTheme.pageGap() * 2));
        }
    }

    private void addRoom() {
        JComboBox<String> type = new JComboBox<>(ROOM_TYPES);
        JSpinner capacity = new JSpinner(new SpinnerNumberModel(PortalTheme.pageGap(), 1, MAX_CAPACITY, 1));
        JTextField features = PortalForms.textField("");
        if (PortalForms.showForm(this, "Add a room", PortalForms.form("Room type", type, "Capacity", capacity,
                "Features (comma-separated)", features))) {
            run("addRoom " + type.getSelectedItem() + " " + capacity.getValue() + " " + features(features));
        }
    }

    private void editRoom() {
        String id = selectedRoomId();
        if (id.isEmpty()) {
            return;
        }
        Room room = model().listRoom(id);
        JComboBox<String> type = new JComboBox<>(ROOM_TYPES);
        type.setSelectedItem(room.getRoomType().toString());
        JSpinner capacity = new JSpinner(new SpinnerNumberModel(room.getCapacity(), 1,
                Math.max(MAX_CAPACITY, room.getCapacity()), 1));
        JTextField features = PortalForms.textField(String.join(",", room.getFeatures()));
        if (!PortalForms.showForm(this, "Edit room " + id, PortalForms.form("Room type", type,
                "Capacity", capacity, "Features (comma-separated)", features))) {
            return;
        }
        if (!room.getRoomType().toString().equals(type.getSelectedItem())) {
            run("modifyRoom " + id + " roomType " + type.getSelectedItem());
        }
        if (room.getCapacity() != (Integer) capacity.getValue()) {
            run("modifyRoom " + id + " capacity " + capacity.getValue());
        }
        if (!String.join(",", room.getFeatures()).equals(features.getText().trim())) {
            run("modifyRoom " + id + " features " + features(features));
        }
    }

    private void deleteRoom() {
        String id = selectedRoomId();
        if (!id.isEmpty() && PortalForms.confirm(this,
                "Delete room " + id + "? Its future bookings will be cancelled.")) {
            run("deleteRoom " + id);
        }
    }

    private static String features(JTextField field) {
        String text = field.getText().trim().replace(" ", "");
        return text.isEmpty() ? "-" : text;
    }
}
