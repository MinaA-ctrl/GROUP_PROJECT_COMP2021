package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/** This is a room booking during any one CROBS session. */
public final class Booking {

    private final String bookingId;
    private final String ownerUserId;
    private String roomId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String purpose;

    /**
     * This creates a booking. The booking manager assigns IDs.
     *
     * @param bookingId the unique booking ID
     * @param ownerUserId the ID of the user who owns the booking
     * @param roomId the booked room
     * @param date the date of the booking
     * @param startTime the start time
     * @param endTime the end time, after the start time
     * @param purpose the purpose of the booking
     * @throws IllegalArgumentException if a value is blank or the times are invalid
     */
    public Booking(String bookingId, String ownerUserId, String roomId,
                   LocalDate date, LocalTime startTime, LocalTime endTime,
                   String purpose) {
        this.bookingId = requireText(bookingId, "bookingId");
        this.ownerUserId = requireText(ownerUserId, "ownerUserId");
        updateDetails(roomId, date, startTime, endTime, purpose);
    }

    /**
     * Returns the unique booking ID.
     *
     * @return the unique booking ID
     */
    public String getBookingId() {
        return bookingId;
    }

    /**
     * Returns the ID of the user who owns the booking.
     *
     * @return the ID of the user who owns the booking
     */
    public String getOwnerUserId() {
        return ownerUserId;
    }

    /**
     * Returns the ID of the booked room.
     *
     * @return the ID of the booked room
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     * Returns the date of the booking.
     *
     * @return the date of the booking
     */
    public LocalDate getDate() {
        return date;
    }

    /**
     * Returns the start time.
     *
     * @return the start time
     */
    public LocalTime getStartTime() {
        return startTime;
    }

    /**
     * Returns the end time.
     *
     * @return the end time
     */
    public LocalTime getEndTime() {
        return endTime;
    }

    /**
     * Returns the purpose of the booking.
     *
     * @return the purpose of the booking
     */
    public String getPurpose() {
        return purpose;
    }

    /**
     * This changes editable details, but it preserves bookingId and ownerUserId.
     *
     * @param roomId the booked room
     * @param date the date of the booking
     * @param startTime the start time
     * @param endTime the end time, after the start time
     * @param purpose the purpose of the booking
     * @throws IllegalArgumentException if a value is blank or the times are invalid
     */
    public void updateDetails(String roomId, LocalDate date,
                              LocalTime startTime, LocalTime endTime,
                              String purpose) {
        String checkedRoomId = requireText(roomId, "roomId");
        LocalDate checkedDate = Objects.requireNonNull(date, "date");
        LocalTime checkedStart = Objects.requireNonNull(startTime, "startTime");
        LocalTime checkedEnd = Objects.requireNonNull(endTime, "endTime");
        String checkedPurpose = requireText(purpose, "purpose");

        if (!checkedStart.isBefore(checkedEnd)) {
            throw new IllegalArgumentException("startTime must be before endTime");
        }

        this.roomId = checkedRoomId;
        this.date = checkedDate;
        this.startTime = checkedStart;
        this.endTime = checkedEnd;
        this.purpose = checkedPurpose;
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
