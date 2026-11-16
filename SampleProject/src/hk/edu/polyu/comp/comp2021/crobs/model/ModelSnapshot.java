package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.List;
import java.util.Map;

/**
 * A saved copy of all users, rooms and bookings (the Memento pattern, used for BON2).
 * A snapshot is taken before every state-changing command, so restoring it reverses the
 * command together with all of its side effects, such as bookings cancelled by deleteRoom.
 */
final class ModelSnapshot {

    private final String description;
    private final Map<String, User> savedAccounts;
    private final List<Room> savedRooms;
    private final Map<String, Booking> savedBookings;

    /**
     * Copies the current state of the three managers.
     *
     * @param description the command this snapshot belongs to, shown after undo or redo
     * @param users the user manager to copy
     * @param rooms the room list to copy
     * @param bookings the booking manager to copy
     */
    ModelSnapshot(String description, UserManager users, RoomList rooms, BookingManager bookings) {
        this.description = description;
        savedAccounts = users.copyAccounts();
        savedRooms = rooms.copyRooms();
        savedBookings = bookings.copyBookings();
    }

    /**
     * Returns the command this snapshot belongs to.
     *
     * @return a short description such as "addRoom meetingRoom 20 tv"
     */
    String getDescription() {
        return description;
    }

    /**
     * Puts the saved state back into the managers. The active login is not changed.
     *
     * @param users the user manager to restore
     * @param rooms the room list to restore
     * @param bookings the booking manager to restore
     */
    void restore(UserManager users, RoomList rooms, BookingManager bookings) {
        users.restoreAccounts(savedAccounts);
        rooms.restoreRooms(savedRooms);
        bookings.restoreBookings(savedBookings);
    }
}
