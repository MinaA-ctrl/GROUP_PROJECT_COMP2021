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

/** Stores bookings and enforces booking rules for one CROBS session. */
public final class BookingManager {

    private final Map<String, Booking> bookings = new HashMap<>();
    private final Supplier<String> currentUserId;
    private final Predicate<String> roomExists;
    private final BooleanSupplier currentUserIsAdmin;
    private long nextBookingId = 1;

    /**
     * Connects bookings to the group's session and room implementations.
     * The supplier must return null when nobody is logged in. The predicate
     * must return true only for a room ID that currently exists.
     */
    public BookingManager(Supplier<String> currentUserId, Predicate<String> roomExists) {
        this(currentUserId, roomExists, () -> false);
    }

    /**
     * Also connects admin privileges to the current session. The admin supplier
     * must reflect the logged-in user's role, including the superadmin role.
     */
    public BookingManager(Supplier<String> currentUserId, Predicate<String> roomExists,
                          BooleanSupplier currentUserIsAdmin) {
        this.currentUserId = Objects.requireNonNull(currentUserId, "currentUserId");
        this.roomExists = Objects.requireNonNull(roomExists, "roomExists");
        this.currentUserIsAdmin = Objects.requireNonNull(currentUserIsAdmin, "currentUserIsAdmin");
    }

    /** Creates a booking owned by the current user (REQ10). */
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
     * Modifies a booking owned by the current user (REQ11).
     * A failed update leaves the stored booking unchanged.
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

    /** Deletes a booking owned by the current user (REQ12). */
    public void deleteBooking(String bookingId) {
        Booking existing = requireOwnedBooking(bookingId);
        bookings.remove(existing.getBookingId());
    }

    /**
     * Returns the current user's bookings sorted by date and start time (REQ17).
     * Returned bookings are copies, so editing them cannot bypass this manager.
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
     * Checks booking availability for a room search (REQ13). Capacity and
     * features are checked separately by the room implementation. Unknown
     * rooms and invalid intervals are rejected rather than reported as available.
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
     * Returns the seven-day schedule starting on the supplied date (REQ14).
     * Admins can see all owner IDs; other users see only their own owner ID.
     * Results are immutable snapshots sorted by date, start time, and booking ID.
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
     * bookings and returns the count. The user manager must validate that the
     * target is a removable regular user before calling this method.
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
     * This also works after the room has been removed from the room collection.
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
