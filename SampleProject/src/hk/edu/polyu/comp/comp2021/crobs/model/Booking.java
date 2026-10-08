package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/** Represents a room booking during one CROBS session. */
public final class Booking {

    private final String bookingId;
    private final String ownerUserId;
    private String roomId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String purpose;

    /** Creates a booking. IDs are assigned by the booking manager. */
    public Booking(String bookingId, String ownerUserId, String roomId,
                   LocalDate date, LocalTime startTime, LocalTime endTime,
                   String purpose) {
        this.bookingId = requireText(bookingId, "bookingId");
        this.ownerUserId = requireText(ownerUserId, "ownerUserId");
        updateDetails(roomId, date, startTime, endTime, purpose);
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getRoomId() {
        return roomId;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getPurpose() {
        return purpose;
    }

    /** Changes editable details while preserving bookingId and ownerUserId. */
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
