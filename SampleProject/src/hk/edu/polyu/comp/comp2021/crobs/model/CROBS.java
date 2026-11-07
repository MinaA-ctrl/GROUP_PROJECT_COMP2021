package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class CROBS {

    private final RoomList rooms;
    private final BookingManager bookings;
    private final Supplier<String> currentUserId;
    private final BooleanSupplier currentUserIsAdmin;
    private final Clock clock;

    /** Starts with no logged-in user until the real user manager is connected. */
    public CROBS() {
        this(new RoomList(), () -> null, () -> false);
    }

    /** Connects one room collection and the live user session to bookings. */
    public CROBS(RoomList rooms, Supplier<String> currentUserId,
                 BooleanSupplier currentUserIsAdmin) {
        this(rooms, currentUserId, currentUserIsAdmin, Clock.systemDefaultZone());
    }

    /** Clock injection keeps room-deletion cutoff behavior reproducible in tests. */
    public CROBS(RoomList rooms, Supplier<String> currentUserId,
                 BooleanSupplier currentUserIsAdmin, Clock clock) {
        this.rooms = Objects.requireNonNull(rooms, "rooms");
        this.currentUserId = Objects.requireNonNull(currentUserId, "currentUserId");
        this.currentUserIsAdmin = Objects.requireNonNull(currentUserIsAdmin, "currentUserIsAdmin");
        this.clock = Objects.requireNonNull(clock, "clock");
        bookings = new BookingManager(currentUserId, rooms::containsRoom, currentUserIsAdmin);
    }

    public BookingManager getBookingManager() {
        return bookings;
    }

    public Room addRoom(String roomType, int capacity, String features) {
        requireAdmin();
        return rooms.addRoom(capacity, RoomType.fromString(roomType), features);
    }

    public Room modifyRoom(String roomId, String field, String newValue) {
        requireAdmin();
        return rooms.modifyRoom(roomId, field, newValue);
    }

    /** Validates permission and the room before cancelling future bookings. */
    public void deleteRoom(String roomId) {
        requireAdmin();
        rooms.getRoom(roomId);
        bookings.cancelFutureBookingsForRoom(roomId, LocalDateTime.now(clock));
        rooms.deleteRoom(roomId);
    }

    public Room listRoom(String roomId) {
        requireLogin();
        return rooms.getRoom(roomId);
    }

    public List<Room> listAllRooms() {
        requireLogin();
        return rooms.getAllRooms();
    }

    /** Combines capacity, all requested features, and booking availability (REQ13). */
    public List<Room> search(LocalDate date, LocalTime start, LocalTime end,
                             int minCapacity, String features) {
        requireLogin();
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        if (minCapacity < 0) {
            throw new IllegalArgumentException("Minimum capacity must not be negative");
        }
        List<String> requiredFeatures = Room.parseFeatures(features);
        List<Room> result = new ArrayList<>();
        for (Room room : rooms.getAllRooms()) {
            if (room.getCapacity() >= minCapacity
                    && room.getFeatures().containsAll(requiredFeatures)
                    && bookings.isRoomAvailable(room.getRoomID(), date, start, end)) {
                result.add(room);
            }
        }
        return result;
    }

    private void requireLogin() {
        String userId = currentUserId.get();
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalStateException("Log in before managing rooms");
        }
    }

    private void requireAdmin() {
        requireLogin();
        if (!currentUserIsAdmin.getAsBoolean()) {
            throw new IllegalStateException("Admin privileges are required");
        }
    }

}
