package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** An entry contains immutable schedule data with the owner's identity hidden when required. */
public final class ScheduleEntry {

    private final String bookingId;
    private final String roomId;
    private final String ownerUserId;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String purpose;

    ScheduleEntry(Booking booking, boolean showOwner) {
        bookingId = booking.getBookingId();
        roomId = booking.getRoomId();
        ownerUserId = showOwner ? booking.getOwnerUserId() : null;
        date = booking.getDate();
        startTime = booking.getStartTime();
        endTime = booking.getEndTime();
        purpose = booking.getPurpose();
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getRoomId() {
        return roomId;
    }

    /** Returns null when the current viewer must not see the owner's ID. */
    public String getOwnerUserId() {
        return ownerUserId;
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
}
