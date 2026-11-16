package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;

/** Our booking manager stores bookings (wow) and enforces booking rules for one CROBS session. */
public final class BookingManager {

    private final Map<String, Booking> bookings = new HashMap<>();
    private final Supplier<String> currentUserId;
    private final Predicate<String> roomExists;
    private final BooleanSupplier currentUserIsAdmin;
    private long nextBookingId = 1;

    /**
     * This connects bookings to the group's session and room implementations.
     * The supplier must return null when nobody is logged in. The predicate
     * must return true only for a room ID that currently exists.
     *
     * @param currentUserId supplies the logged-in user's ID, or null
     * @param roomExists tests whether a room ID exists
     */
    public BookingManager(Supplier<String> currentUserId, Predicate<String> roomExists) {
        this(currentUserId, roomExists, () -> false);
    }

    /**
     * Also connects admin privileges to the current session. The admin supplier
     * must reflect the logged-in user's role, including the superadmin role.
     *
     * @param currentUserId supplies the logged-in user's ID, or null
     * @param roomExists tests whether a room ID exists
     * @param currentUserIsAdmin tells whether the logged-in user is an admin
     */
    public BookingManager(Supplier<String> currentUserId, Predicate<String> roomExists,
                          BooleanSupplier currentUserIsAdmin) {
        this.currentUserId = Objects.requireNonNull(currentUserId, "currentUserId");
        this.roomExists = Objects.requireNonNull(roomExists, "roomExists");
        this.currentUserIsAdmin = Objects.requireNonNull(currentUserIsAdmin, "currentUserIsAdmin");
    }

    /**
     * This creates a booking owned by the current user (REQ10).
     *
     * @param roomId the room to book
     * @param date the date of the booking
     * @param startTime the start time
     * @param endTime the end time
     * @param purpose the purpose of the booking
     * @return a copy of the new booking
     * @throws IllegalStateException if nobody is logged in
     * @throws IllegalArgumentException if the room is unknown, the values are invalid or the slot is taken
     */
    public Booking book(String roomId, LocalDate date, LocalTime startTime,
                        LocalTime endTime, String purpose) {
        String ownerId = requireCurrentUser();
        Booking candidate = new Booking("B" + nextBookingId, ownerId, roomId,
                date, startTime, endTime, purpose);
        validateAvailability(candidate, null);
        bookings.put(candidate.getBookingId(), candidate);
        nextBookingId++;
        return copy(candidate);
    }

    /**
     * This modifies a booking owned by the current user (REQ11).
     * A failed update leaves the stored booking unchanged.
     *
     * @param bookingId the booking to modify
     * @param roomId the new room
     * @param date the new date
     * @param startTime the new start time
     * @param endTime the new end time
     * @param purpose the new purpose
     * @return a copy of the modified booking
     * @throws IllegalStateException if nobody is logged in or the booking belongs to another user
     * @throws IllegalArgumentException if the booking or room is unknown, a value is invalid or the slot is taken
     */
    public Booking modifyBooking(String bookingId, String roomId, LocalDate date,
                                 LocalTime startTime, LocalTime endTime, String purpose) {
        Booking existing = requireOwnedBooking(bookingId);
        Booking candidate = new Booking(existing.getBookingId(), existing.getOwnerUserId(),
                roomId, date, startTime, endTime, purpose);
        validateAvailability(candidate, bookingId);
        bookings.put(bookingId, candidate);
        return copy(candidate);
    }

    /**
     * This deletes a booking owned by the current user (REQ12).
     *
     * @param bookingId the booking to delete
     * @throws IllegalStateException if nobody is logged in or the booking belongs to another user
     * @throws IllegalArgumentException if the booking is unknown
     */
    public void deleteBooking(String bookingId) {
        Booking existing = requireOwnedBooking(bookingId);
        bookings.remove(existing.getBookingId());
    }

    /**
     * This returns the current user's bookings sorted by date and start time (REQ17).
     * Returned bookings are copies, so editing them cannot bypass this manager.
     *
     * @return the current user's bookings
     * @throws IllegalStateException if nobody is logged in
     */
    public List<Booking> listBookings() {
        String ownerId = requireCurrentUser();
        List<Booking> result = new ArrayList<>();
        for (Booking booking : bookings.values()) {
            if (booking.getOwnerUserId().equals(ownerId)) {
                result.add(copy(booking));
            }
        }
        result.sort(Comparator.comparing(Booking::getDate)
                .thenComparing(Booking::getStartTime)
                .thenComparing(Booking::getBookingId));
        return result;
    }

    /**
     * This checks booking availability for a room search (REQ13). Capacity and
     * features are checked separately by the room implementation. Unknown
     * rooms and invalid intervals are rejected rather than reported as available.
     *
     * @param roomId the room to check
     * @param date the date
     * @param startTime the start time
     * @param endTime the end time
     * @return true if no booking of the room overlaps the interval
     * @throws IllegalStateException if nobody is logged in
     * @throws IllegalArgumentException if the room is unknown or the interval is invalid
     */
    public boolean isRoomAvailable(String roomId, LocalDate date,
                                   LocalTime startTime, LocalTime endTime) {
        requireCurrentUser();
        requireRoom(roomId);
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(endTime, "endTime");
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }
        return !hasConflict(roomId, date, startTime, endTime, null);
    }

    /**
     * This returns the seven-day schedule starting on the supplied date (REQ14).
     * Admins can see ALL owner IDs, but other users see only their own owner ID.
     * Results are immutable snapshots sorted by date, start time, and booking ID.
     *
     * @param roomId the room whose schedule is shown
     * @param weekStartDate the first day of the seven-day period
     * @return the bookings of the room in that period
     * @throws IllegalStateException if nobody is logged in
     * @throws IllegalArgumentException if the room is unknown
     */
    public List<ScheduleEntry> schedule(String roomId, LocalDate weekStartDate) {
        String viewerId = requireCurrentUser();
        requireRoom(roomId);
        Objects.requireNonNull(weekStartDate, "weekStartDate");
        LocalDate weekEndDate = weekStartDate.plusDays(6);
        boolean admin = currentUserIsAdmin.getAsBoolean();
        List<ScheduleEntry> result = new ArrayList<>();
        for (Booking booking : bookings.values()) {
            if (booking.getRoomId().equals(roomId)
                    && !booking.getDate().isBefore(weekStartDate)
                    && !booking.getDate().isAfter(weekEndDate)) {
                boolean showOwner = admin || booking.getOwnerUserId().equals(viewerId);
                result.add(new ScheduleEntry(booking, showOwner));
            }
        }
        result.sort(Comparator.comparing(ScheduleEntry::getDate)
                .thenComparing(ScheduleEntry::getStartTime)
                .thenComparing(ScheduleEntry::getBookingId));
        return Collections.unmodifiableList(result);
    }

    /**
     * Admin integration hook for removing a user (REQ3). Cancels all their
     * bookings and returns the count. The user manager MUST validate that the
     * target is a removable regular user before calling this method.
     *
     * @param userId the user whose bookings are cancelled
     * @return the number of cancelled bookings
     * @throws IllegalStateException if the current user is not an admin
     */
    public int cancelBookingsForUser(String userId) {
        requireAdmin();
        requireText(userId, "userId");
        int previousSize = bookings.size();
        bookings.values().removeIf(booking -> booking.getOwnerUserId().equals(userId));
        return previousSize - bookings.size();
    }

    /**
     * Admin integration hook for deleting a room (REQ9). Cancels bookings whose
     * start is at or after the cutoff, retaining past and ongoing bookings.
     * The caller supplies the current time once for the whole deletion.
     * This also works after the room has been removed from the room collection!
     *
     * @param roomId the deleted room
     * @param cutoff bookings starting at or after this time are cancelled
     * @return the number of cancelled bookings
     * @throws IllegalStateException if the current user is not an admin
     */
    public int cancelFutureBookingsForRoom(String roomId, LocalDateTime cutoff) {
        requireAdmin();
        requireText(roomId, "roomId");
        Objects.requireNonNull(cutoff, "cutoff");
        int previousSize = bookings.size();
        bookings.values().removeIf(booking -> booking.getRoomId().equals(roomId)
                && !booking.getDate().atTime(booking.getStartTime()).isBefore(cutoff));
        return previousSize - bookings.size();
    }

    /**
     * Copies every booking for an undo snapshot. The booking ID counter is not part of
     * the snapshot, so booking IDs are never reused after an undo.
     *
     * @return a new map from booking ID to an independent copy of the booking
     */
    Map<String, Booking> copyBookings() {
        Map<String, Booking> copies = new HashMap<>();
        for (Booking booking : bookings.values()) {
            copies.put(booking.getBookingId(), copy(booking));
        }
        return copies;
    }

    /**
     * Replaces all bookings with copies of saved ones.
     *
     * @param savedBookings the bookings to restore
     */
    void restoreBookings(Map<String, Booking> savedBookings) {
        bookings.clear();
        for (Booking booking : savedBookings.values()) {
            bookings.put(booking.getBookingId(), copy(booking));
        }
    }

    private void requireAdmin() {
        requireCurrentUser();
        if (!currentUserIsAdmin.getAsBoolean()) {
            throw new IllegalStateException("Admin privileges are required");
        }
    }

    private static void requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private void requireRoom(String roomId) {
        requireText(roomId, "roomId");
        if (!roomExists.test(roomId)) {
            throw new IllegalArgumentException("Unknown room ID: " + roomId);
        }
    }

    private String requireCurrentUser() {
        String userId = currentUserId.get();
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalStateException("Log in before managing bookings");
        }
        return userId;
    }

    private Booking requireOwnedBooking(String bookingId) {
        String userId = requireCurrentUser();
        Booking booking = bookings.get(bookingId);
        if (booking == null) {
            throw new IllegalArgumentException("Unknown booking ID: " + bookingId);
        }
        if (!booking.getOwnerUserId().equals(userId)) {
            throw new IllegalStateException("You can only manage your own bookings");
        }
        return booking;
    }

    private void validateAvailability(Booking candidate, String excludedBookingId) {
        requireRoom(candidate.getRoomId());
        if (hasConflict(candidate.getRoomId(), candidate.getDate(), candidate.getStartTime(),
                candidate.getEndTime(), excludedBookingId)) {
            throw new IllegalArgumentException("Room is already booked during that time");
        }
    }

    private boolean hasConflict(String roomId, LocalDate date, LocalTime startTime,
                                LocalTime endTime, String excludedBookingId) {
        for (Booking existing : bookings.values()) {
            if (!existing.getBookingId().equals(excludedBookingId)
                    && existing.getRoomId().equals(roomId)
                    && existing.getDate().equals(date)
                    && startTime.isBefore(existing.getEndTime())
                    && existing.getStartTime().isBefore(endTime)) {
                return true;
            }
        }
        return false;
    }

    private static Booking copy(Booking booking) {
        return new Booking(booking.getBookingId(), booking.getOwnerUserId(), booking.getRoomId(),
                booking.getDate(), booking.getStartTime(), booking.getEndTime(), booking.getPurpose());
    }
}
