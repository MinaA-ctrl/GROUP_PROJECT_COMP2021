package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class CROBS {

    private final RoomList rooms;
    private final BookingManager bookings;

    /** Starts with no logged-in user until the real user manager is connected. */
    public CROBS() {
        this(new RoomList(), () -> null, () -> false);
    }

    /** Connects one room collection and the live user session to bookings. */
    public CROBS(RoomList rooms, Supplier<String> currentUserId,
                 BooleanSupplier currentUserIsAdmin) {
        this.rooms = Objects.requireNonNull(rooms, "rooms");
        bookings = new BookingManager(currentUserId, this::roomExists, currentUserIsAdmin);
    }

    public BookingManager getBookingManager() {
        return bookings;
    }

    private boolean roomExists(String roomId) {
        // RoomList.getRoom currently throws for an unknown ID.
        try {
            rooms.getRoom(roomId);
            return true;
        } catch (IllegalArgumentException unknownRoom) {
            return false;
        }
    }

}
