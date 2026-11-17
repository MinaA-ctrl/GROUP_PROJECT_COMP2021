package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The single entry point into the model. CROBS coordinates the user, room and booking
 * managers, checks room permissions and keeps the undo/redo history of the session.
 */
public class CROBS {

    private final RoomList rooms;
    private final BookingManager bookings;
    private final UserManager users = new UserManager();
    private final UndoHistory history = new UndoHistory();
    private final Clock clock;

    /** CROBS starts with one superadmin account and no active login. */
    public CROBS() {
        this(new RoomList(), Clock.systemDefaultZone());
    }

    /**
     * The clock controls the cutoff for cancelling future bookings.
     *
     * @param rooms the room collection to use
     * @param clock the clock that gives the current time
     */
    CROBS(RoomList rooms, Clock clock) {
        this.rooms = Objects.requireNonNull(rooms, "rooms");
        this.clock = Objects.requireNonNull(clock, "clock");
        bookings = new BookingManager(users::getCurrentUserId, rooms::containsRoom, users::isCurrentUserAdmin);
    }

    /**
     * Returns the booking manager. Changes made directly through it are not recorded for undo.
     *
     * @return the booking manager of this session
     */
    public BookingManager getBookingManager() {
        return bookings;
    }

    /**
     * Returns the ID of the superadmin account.
     *
     * @return the superadmin user ID
     */
    public String getSuperadminId() {
        return UserManager.superadminId();
    }

    /**
     * Returns the logged-in user.
     *
     * @return the current user, or an empty optional when nobody is logged in
     */
    public Optional<User> getCurrentUser() {
        return users.getCurrentUser();
    }

    /**
     * Creates a regular user (REQ2).
     *
     * @param name the display name
     * @return the new user
     * @throws IllegalStateException if the current user is not an admin
     * @throws IllegalArgumentException if the name is blank
     */
    public User addUser(String name) {
        return recordChange("addUser " + name, () -> users.addUser(name));
    }

    /**
     * Removes a regular user and cancels all of that user's bookings (REQ3).
     *
     * @param userId the user to remove
     * @throws IllegalStateException if the current user is not an admin
     * @throws IllegalArgumentException if the user is unknown or not regular
     */
    public void removeUser(String userId) {
        recordChange("removeUser " + userId, () -> users.removeUser(userId, bookings::cancelBookingsForUser));
    }

    /**
     * Changes the role of a user (REQ4).
     *
     * @param userId the user whose role changes
     * @param newRole regular or admin
     * @return the user with the new role
     * @throws IllegalStateException if the current user is not the superadmin
     * @throws IllegalArgumentException if the user or role is invalid
     */
    public User changeRole(String userId, String newRole) {
        return recordChange("changeRole " + userId + " " + newRole, () -> users.changeRole(userId, newRole));
    }

    /**
     * Logs in as an existing user (REQ5). The undo history of the previous login is cleared,
     * so users can only undo their own commands.
     *
     * @param userId the user to log in as
     * @return the logged-in user
     * @throws IllegalArgumentException if the user is unknown
     */
    public User login(String userId) {
        User user = users.login(userId);
        history.clear();
        return user;
    }

    /**
     * Logs out the current user (REQ6) and clears the undo history.
     *
     * @throws IllegalStateException if nobody is logged in
     */
    public void logout() {
        users.logout();
        history.clear();
    }

    /**
     * Creates a room with a generated ID (REQ7).
     *
     * @param roomType lectureTheatre, computerLab, seminarRoom or meetingRoom
     * @param capacity the positive capacity
     * @param features comma-separated features without spaces, or - for none
     * @return the new room
     * @throws IllegalStateException if the current user is not an admin
     * @throws IllegalArgumentException if any argument is invalid
     */
    public Room addRoom(String roomType, int capacity, String features) {
        requireAdmin();
        return recordChange("addRoom " + roomType + " " + capacity + " " + features,
                () -> rooms.addRoom(capacity, RoomType.fromString(roomType), features));
    }

    /**
     * Changes one field of a room (REQ8).
     *
     * @param roomId the room to modify
     * @param field roomType, capacity or features
     * @param newValue the new value
     * @return the modified room
     * @throws IllegalStateException if the current user is not an admin
     * @throws IllegalArgumentException if the room, field or value is invalid
     */
    public Room modifyRoom(String roomId, String field, String newValue) {
        requireAdmin();
        return recordChange("modifyRoom " + roomId + " " + field + " " + newValue,
                () -> rooms.modifyRoom(roomId, field, newValue));
    }

    /**
     * Deletes a room and cancels its future bookings (REQ9). Permission and the room are
     * validated before anything is cancelled.
     *
     * @param roomId the room to delete
     * @throws IllegalStateException if the current user is not an admin
     * @throws IllegalArgumentException if the room is unknown
     */
    public void deleteRoom(String roomId) {
        requireAdmin();
        rooms.getRoom(roomId);
        recordChange("deleteRoom " + roomId, () -> {
            bookings.cancelFutureBookingsForRoom(roomId, LocalDateTime.now(clock));
            rooms.deleteRoom(roomId);
        });
    }

    /**
     * Creates a booking for the current user (REQ10).
     *
     * @param roomId the room to book
     * @param date the date
     * @param startTime the start time
     * @param endTime the end time
     * @param purpose the purpose
     * @return the new booking
     * @throws IllegalStateException if nobody is logged in
     * @throws IllegalArgumentException if a value is invalid or the slot is taken
     */
    public Booking book(String roomId, LocalDate date, LocalTime startTime, LocalTime endTime, String purpose) {
        return recordChange("book " + roomId + " " + date + " " + startTime + " " + endTime,
                () -> bookings.book(roomId, date, startTime, endTime, purpose));
    }

    /**
     * Changes a booking owned by the current user (REQ11).
     *
     * @param bookingId the booking to change
     * @param roomId the new room
     * @param date the new date
     * @param startTime the new start time
     * @param endTime the new end time
     * @param purpose the new purpose
     * @return the modified booking
     * @throws IllegalStateException if nobody is logged in or the booking is not the user's
     * @throws IllegalArgumentException if a value is invalid or the slot is taken
     */
    public Booking modifyBooking(String bookingId, String roomId, LocalDate date,
                                 LocalTime startTime, LocalTime endTime, String purpose) {
        return recordChange("modifyBooking " + bookingId,
                () -> bookings.modifyBooking(bookingId, roomId, date, startTime, endTime, purpose));
    }

    /**
     * Deletes a booking owned by the current user (REQ12).
     *
     * @param bookingId the booking to delete
     * @throws IllegalStateException if nobody is logged in or the booking is not the user's
     * @throws IllegalArgumentException if the booking is unknown
     */
    public void deleteBooking(String bookingId) {
        recordChange("deleteBooking " + bookingId, () -> bookings.deleteBooking(bookingId));
    }

    /**
     * Lists the current user's bookings sorted by date and start time (REQ17).
     *
     * @return copies of the user's bookings
     * @throws IllegalStateException if nobody is logged in
     */
    public List<Booking> listBookings() {
        return bookings.listBookings();
    }

    /**
     * Lists the bookings of a room in a seven-day period (REQ14).
     *
     * @param roomId the room
     * @param weekStartDate the first day of the period
     * @return the bookings sorted by date and start time
     * @throws IllegalStateException if nobody is logged in
     * @throws IllegalArgumentException if the room is unknown
     */
    public List<ScheduleEntry> schedule(String roomId, LocalDate weekStartDate) {
        return bookings.schedule(roomId, weekStartDate);
    }

    /**
     * Returns one room (REQ15).
     *
     * @param roomId the room ID
     * @return the room
     * @throws IllegalStateException if nobody is logged in
     * @throws IllegalArgumentException if the room is unknown
     */
    public Room listRoom(String roomId) {
        requireLogin();
        return rooms.getRoom(roomId);
    }

    /**
     * Returns all rooms sorted by room ID (REQ16).
     *
     * @return the rooms
     * @throws IllegalStateException if nobody is logged in
     */
    public List<Room> listAllRooms() {
        requireLogin();
        return rooms.getAllRooms();
    }

    /**
     * Here, it combines capacity, all requested features, and booking availability (REQ13).
     *
     * @param date the date
     * @param start the start time
     * @param end the end time
     * @param minCapacity the minimum capacity
     * @param features the required features, or - for none
     * @return the matching rooms sorted by room ID
     * @throws IllegalStateException if nobody is logged in
     * @throws IllegalArgumentException if the interval, capacity or features are invalid
     */
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

    /**
     * Undoes the most recent state-changing command of the current login session (BON2).
     *
     * @return a description of the undone command
     * @throws IllegalStateException if nobody is logged in or there is nothing to undo
     */
    public String undo() {
        requireLogin("Log in before using undo");
        ModelSnapshot before = history.takeUndo();
        history.pushRedo(snapshot(before.getDescription()));
        before.restore(users, rooms, bookings);
        return before.getDescription();
    }

    /**
     * Redoes the most recently undone command (BON2).
     *
     * @return a description of the redone command
     * @throws IllegalStateException if nobody is logged in or there is nothing to redo
     */
    public String redo() {
        requireLogin("Log in before using redo");
        ModelSnapshot after = history.takeRedo();
        history.pushUndo(snapshot(after.getDescription()));
        after.restore(users, rooms, bookings);
        return after.getDescription();
    }

    /**
     * Checks whether undo is currently possible.
     *
     * @return true if there is a command to undo
     */
    public boolean canUndo() {
        return history.canUndo();
    }

    /**
     * Checks whether redo is currently possible.
     *
     * @return true if there is an undone command to redo
     */
    public boolean canRedo() {
        return history.canRedo();
    }

    /** Runs a change and records the previous state only if the change succeeds. */
    private <T> T recordChange(String description, Supplier<T> change) {
        ModelSnapshot before = snapshot(description);
        T result = change.get();
        history.recordChange(before);
        return result;
    }

    private void recordChange(String description, Runnable change) {
        ModelSnapshot before = snapshot(description);
        change.run();
        history.recordChange(before);
    }

    private ModelSnapshot snapshot(String description) {
        return new ModelSnapshot(description, users, rooms, bookings);
    }

    private void requireLogin() {
        requireLogin("Log in before managing rooms");
    }

    private void requireLogin(String message) {
        if (!users.getCurrentUser().isPresent()) {
            throw new IllegalStateException(message);
        }
    }

    private void requireAdmin() {
        requireLogin();
        if (!users.isCurrentUserAdmin()) {
            throw new IllegalStateException("Admin privileges are required");
        }
    }

}
