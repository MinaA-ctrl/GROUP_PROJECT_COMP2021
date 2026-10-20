package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingManagerTest {

    private final LocalDate date = LocalDate.of(2026, 10, 25);
    private final AtomicReference<String> currentUser = new AtomicReference<>("U1");
    private final BookingManager manager = new BookingManager(currentUser::get,
            roomId -> Arrays.asList("R1", "R2").contains(roomId));

    private Booking book(String roomId, int startHour, int endHour) {
        return manager.book(roomId, date, LocalTime.of(startHour, 0),
                LocalTime.of(endHour, 0), "Project meeting");
    }

    @Test
    void requiresLoginForEveryUserOperation() {
        Booking booking = book("R1", 9, 10);
        currentUser.set(null);
        assertThrows(IllegalStateException.class, () -> book("R1", 10, 11));
        assertThrows(IllegalStateException.class, manager::listBookings);
        assertThrows(IllegalStateException.class, () -> manager.deleteBooking(booking.getBookingId()));
        assertThrows(IllegalStateException.class, () -> manager.modifyBooking(booking.getBookingId(),
                "R1", date, LocalTime.of(10, 0), LocalTime.of(11, 0), "Changed"));
    }

    @Test
    void rejectsUnknownRoomsAndInvalidIntervals() {
        assertThrows(IllegalArgumentException.class, () -> book("missing", 9, 10));
        assertThrows(IllegalArgumentException.class, () -> book("R1", 10, 10));
        assertThrows(IllegalArgumentException.class, () -> book("R1", 11, 10));
        assertTrue(manager.listBookings().isEmpty());
    }

    @Test
    void detectsPartialContainingAndContainedOverlaps() {
        book("R1", 10, 12);
        assertThrows(IllegalArgumentException.class, () -> book("R1", 9, 11));
        assertThrows(IllegalArgumentException.class, () -> book("R1", 11, 13));
        assertThrows(IllegalArgumentException.class, () -> book("R1", 9, 13));
        assertThrows(IllegalArgumentException.class, () -> book("R1", 10, 11));
        currentUser.set("U2");
        assertThrows(IllegalArgumentException.class, () -> book("R1", 10, 12));
    }

    @Test
    void allowsAdjacentBookingsOtherRoomsAndOtherDates() {
        book("R1", 10, 12);
        book("R1", 9, 10);
        book("R1", 12, 13);
        book("R2", 10, 12);
        manager.book("R1", date.plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0), "Next day");
        assertEquals(5, manager.listBookings().size());
    }

    @Test
    void onlyOwnerCanModifyOrDeleteAndListShowsOnlyTheirBookings() {
        Booking first = book("R1", 9, 10);
        currentUser.set("U2");
        Booking second = book("R1", 10, 11);
        assertThrows(IllegalStateException.class, () -> manager.deleteBooking(first.getBookingId()));
        assertThrows(IllegalStateException.class, () -> manager.modifyBooking(first.getBookingId(),
                "R2", date, LocalTime.of(9, 0), LocalTime.of(10, 0), "Changed"));
        assertEquals(second.getBookingId(), manager.listBookings().get(0).getBookingId());
        assertThrows(IllegalArgumentException.class, () -> manager.deleteBooking("missing"));
    }

    @Test
    void modificationPreservesIdentityExcludesItselfAndRejectsConflictsAtomically() {
        Booking first = book("R1", 9, 10);
        book("R1", 11, 12);
        Booking changed = manager.modifyBooking(first.getBookingId(), "R1", date,
                LocalTime.of(9, 0), LocalTime.of(11, 0), "Longer meeting");
        assertEquals(first.getBookingId(), changed.getBookingId());
        assertEquals(first.getOwnerUserId(), changed.getOwnerUserId());
        assertThrows(IllegalArgumentException.class, () -> manager.modifyBooking(first.getBookingId(),
                "R1", date, LocalTime.of(10, 0), LocalTime.of(12, 0), "Conflict"));
        assertThrows(IllegalArgumentException.class, () -> manager.modifyBooking(first.getBookingId(),
                "missing", date, LocalTime.of(9, 0), LocalTime.of(10, 0), "Unknown room"));
        assertEquals("Longer meeting", manager.listBookings().get(0).getPurpose());
        assertEquals(LocalTime.of(11, 0), manager.listBookings().get(0).getEndTime());
    }

    @Test
    void deletionFreesRoomAndDoesNotReuseIds() {
        Booking first = book("R1", 9, 10);
        manager.deleteBooking(first.getBookingId());
        assertTrue(manager.listBookings().isEmpty());
        Booking second = book("R1", 9, 10);
        assertNotEquals(first.getBookingId(), second.getBookingId());
    }

    @Test
    void listIsSortedAndReturnedObjectsCannotChangeStoredBookings() {
        Booking later = manager.book("R1", date.plusDays(1), LocalTime.of(9, 0),
                LocalTime.of(10, 0), "Tomorrow");
        Booking afternoon = book("R1", 14, 15);
        Booking morning = book("R1", 9, 10);
        List<Booking> listed = manager.listBookings();
        assertEquals(morning.getBookingId(), listed.get(0).getBookingId());
        assertEquals(afternoon.getBookingId(), listed.get(1).getBookingId());
        assertEquals(later.getBookingId(), listed.get(2).getBookingId());
        listed.get(0).updateDetails("R2", date, LocalTime.of(9, 0), LocalTime.of(10, 0), "Detached");
        morning.updateDetails("R2", date, LocalTime.of(9, 0), LocalTime.of(10, 0), "Also detached");
        listed.clear();
        assertEquals(3, manager.listBookings().size());
        assertEquals("R1", manager.listBookings().get(0).getRoomId());
    }
}
