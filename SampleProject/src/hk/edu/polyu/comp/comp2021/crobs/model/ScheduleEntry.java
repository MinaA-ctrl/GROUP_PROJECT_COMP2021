package hk.edu.polyu.comp.comp2021.crobs.model;

import java.time.LocalDate;
import java.time.LocalTime;

/** An entry contains immutable schedule data with the owner's identity hidden when required. */
public final class ScheduleEntry {

    private final String bookingId;
    private final String roomId;
    private final String ownerUserId;
    private final boolean ownerVisible;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String purpose;

    /**
     * Creates a schedule snapshot of a booking.
     *
     * @param booking the booking to show
     * @param showOwner whether the viewer may see the owner's user ID
     */
    ScheduleEntry(Booking booking, boolean showOwner) {
        bookingId = booking.getBookingId();
        roomId = booking.getRoomId();
        ownerUserId = booking.getOwnerUserId();
        ownerVisible = showOwner;
        date = booking.getDate();
        startTime = booking.getStartTime();
        endTime = booking.getEndTime();
        purpose = booking.getPurpose();
    }

    /**
     * Returns the booking ID.
     *
     * @return the booking ID
     */
    public String getBookingId() {
        return bookingId;
    }

    /**
     * Returns the room ID.
     *
     * @return the booked room's ID
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     * Returns the owner's user ID if the viewer may see it.
     *
     * @return the owner's ID, or null when the current viewer must not see it
     */
    public String getOwnerUserId() {
        return ownerVisible ? ownerUserId : null;
    }

    /**
     * Returns the date.
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
     * Returns the purpose.
     *
     * @return the purpose of the booking
     */
    public String getPurpose() {
        return purpose;
    }
}
