package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CROBS {

    private final RoomList rooms;
    private final BookingManager bookings;
    private final UserManager users = new UserManager();
    private final Clock clock;

    /** CROBS starts with one superadmin account and no active login. */
    public CROBS() {
        this(new RoomList(), Clock.systemDefaultZone());
    }

    /** The clock controls the cutoff for cancelling future bookings. */
    CROBS(RoomList rooms, Clock clock) {
        this.rooms = Objects.requireNonNull(rooms, "rooms");
        this.clock = Objects.requireNonNull(clock, "clock");
        bookings = new BookingManager(users::getCurrentUserId, rooms::containsRoom, users::isCurrentUserAdmin);
    }

    public BookingManager getBookingManager() {
        return bookings;
    }

    public String getSuperadminId() {
        return UserManager.SUPERADMIN_ID;
    }

    public User addUser(String name) {
        return users.addUser(name);
    }

    public void removeUser(String userId) {
        users.removeUser(userId, bookings::cancelBookingsForUser);
    }

    public User changeRole(String userId, String newRole) {
        return users.changeRole(userId, newRole);
    }

    public User login(String userId) {
        return users.login(userId);
    }

    public void logout() {
        users.logout();
    }

    public Room addRoom(String roomType, int capacity, String features) {
        requireAdmin();
        return rooms.addRoom(capacity, RoomType.fromString(roomType), features);
    }

    public Room modifyRoom(String roomId, String field, String newValue) {
        requireAdmin();
        return rooms.modifyRoom(roomId, field, newValue);
    }

    /** Deleting validates permission and the room before cancelling future bookings. */
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

    /** Here, it combines capacity, all requested features, and booking availability (REQ13). */
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
        if (users.getCurrentUserId() == null) {
            throw new IllegalStateException("Log in before managing rooms");
        }
    }

    private void requireAdmin() {
        requireLogin();
        if (!users.isCurrentUserAdmin()) {
            throw new IllegalStateException("Admin privileges are required");
        }
    }

}
