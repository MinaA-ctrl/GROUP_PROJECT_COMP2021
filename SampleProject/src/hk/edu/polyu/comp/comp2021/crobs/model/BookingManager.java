package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Stores bookings and enforces booking rules for one CROBS session. */
public final class BookingManager {

    private final Map<String, Booking> bookings = new HashMap<>();
    private final Supplier<String> currentUserId;
    private final Predicate<String> roomExists;
    private long nextBookingId = 1;

    /**
     * Connects bookings to the group's session and room implementations.
     * The supplier must return null when nobody is logged in. The predicate
     * must return true only for a room ID that currently exists.
     */
    public BookingManager(Supplier<String> currentUserId, Predicate<String> roomExists) {
        this.currentUserId = Objects.requireNonNull(currentUserId, "currentUserId");
        this.roomExists = Objects.requireNonNull(roomExists, "roomExists");
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
        if (!roomExists.test(candidate.getRoomId())) {
            throw new IllegalArgumentException("Unknown room ID: " + candidate.getRoomId());
        }
        for (Booking existing : bookings.values()) {
            if (!existing.getBookingId().equals(excludedBookingId)
                    && existing.getRoomId().equals(candidate.getRoomId())
                    && existing.getDate().equals(candidate.getDate())
                    && candidate.getStartTime().isBefore(existing.getEndTime())
                    && existing.getStartTime().isBefore(candidate.getEndTime())) {
                throw new IllegalArgumentException("Room is already booked during that time");
            }
        }
    }

    private static Booking copy(Booking booking) {
        return new Booking(booking.getBookingId(), booking.getOwnerUserId(), booking.getRoomId(),
                booking.getDate(), booking.getStartTime(), booking.getEndTime(), booking.getPurpose());
    }
}
