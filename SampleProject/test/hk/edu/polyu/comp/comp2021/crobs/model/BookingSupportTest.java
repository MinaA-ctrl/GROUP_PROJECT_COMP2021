package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingSupportTest {

    private final LocalDate date = LocalDate.of(2026, 11, 3);
    private final AtomicReference<String> currentUser = new AtomicReference<>("U1");
    private final AtomicBoolean admin = new AtomicBoolean(false);
    private final Set<String> rooms = new HashSet<>(Arrays.asList("R1", "R2"));
    private final BookingManager manager = new BookingManager(currentUser::get,
            rooms::contains, admin::get);

    private Booking book(String roomId, LocalDate day, int start, int end) {
        return manager.book(roomId, day, LocalTime.of(start, 0), LocalTime.of(end, 0), "Meeting");
    }

    @Test
    void availabilityMatchesBookingConflictsAndAllowsAdjacentSlots() {
        Booking booking = book("R1", date, 10, 12);
        assertFalse(manager.isRoomAvailable("R1", date, LocalTime.of(9, 0), LocalTime.of(11, 0)));
        assertFalse(manager.isRoomAvailable("R1", date, LocalTime.of(9, 0), LocalTime.of(13, 0)));
        assertTrue(manager.isRoomAvailable("R1", date, LocalTime.of(9, 0), LocalTime.of(10, 0)));
        assertTrue(manager.isRoomAvailable("R1", date, LocalTime.of(12, 0), LocalTime.of(13, 0)));
        assertTrue(manager.isRoomAvailable("R2", date, LocalTime.of(10, 0), LocalTime.of(12, 0)));
        assertTrue(manager.isRoomAvailable("R1", date.plusDays(1), LocalTime.of(10, 0), LocalTime.of(12, 0)));
        manager.deleteBooking(booking.getBookingId());
        assertTrue(manager.isRoomAvailable("R1", date, LocalTime.of(10, 0), LocalTime.of(12, 0)));
    }

    @Test
    void queriesRejectUnknownRoomsAndInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> manager.schedule("missing", date));
        assertThrows(IllegalArgumentException.class, () -> manager.isRoomAvailable("missing", date,
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        assertThrows(IllegalArgumentException.class, () -> manager.isRoomAvailable("R1", date,
                LocalTime.of(10, 0), LocalTime.of(10, 0)));
        assertThrows(IllegalArgumentException.class, () -> manager.isRoomAvailable("R1", date,
                LocalTime.of(11, 0), LocalTime.of(10, 0)));
        assertThrows(NullPointerException.class, () -> manager.schedule("R1", null));
        assertThrows(NullPointerException.class, () -> manager.isRoomAvailable("R1", null,
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
    }

    @Test
    void scheduleUsesExactlySevenDaysAndSortsByDateThenStartTime() {
        book("R1", date.minusDays(1), 9, 10);
        Booking lastDay = book("R1", date.plusDays(6), 9, 10);
        book("R1", date.plusDays(7), 9, 10);
        book("R2", date, 9, 10);
        Booking afternoon = book("R1", date, 14, 15);
        Booking morning = book("R1", date, 9, 10);
        List<ScheduleEntry> schedule = manager.schedule("R1", date);
        assertEquals(3, schedule.size());
        assertEquals(morning.getBookingId(), schedule.get(0).getBookingId());
        assertEquals(afternoon.getBookingId(), schedule.get(1).getBookingId());
        assertEquals(lastDay.getBookingId(), schedule.get(2).getBookingId());
    }

    @Test
    void scheduleHidesOtherOwnersAndReadsCurrentAdminRoleEachTime() {
        book("R1", date, 9, 10);
        currentUser.set("U2");
        book("R1", date, 10, 11);
        List<ScheduleEntry> regularView = manager.schedule("R1", date);
        assertNull(regularView.get(0).getOwnerUserId());
        assertEquals("U2", regularView.get(1).getOwnerUserId());
        admin.set(true);
        assertEquals("U1", manager.schedule("R1", date).get(0).getOwnerUserId());
        admin.set(false);
        assertNull(manager.schedule("R1", date).get(0).getOwnerUserId());
        assertNull(regularView.get(0).getOwnerUserId());
    }

    @Test
    void scheduleSnapshotsCannotBeEditedAndRemainStableAfterModification() {
        Booking booking = book("R1", date, 9, 10);
        List<ScheduleEntry> snapshot = manager.schedule("R1", date);
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        manager.modifyBooking(booking.getBookingId(), "R2", date,
                LocalTime.of(11, 0), LocalTime.of(12, 0), "Changed");
        assertEquals("R1", snapshot.get(0).getRoomId());
        assertEquals(date, snapshot.get(0).getDate());
        assertEquals(LocalTime.of(9, 0), snapshot.get(0).getStartTime());
        assertEquals(LocalTime.of(10, 0), snapshot.get(0).getEndTime());
        assertEquals("Meeting", snapshot.get(0).getPurpose());
        assertTrue(manager.schedule("R1", date).isEmpty());
    }

    @Test
    void removingUserCancelsOnlyThatUsersBookingsAcrossRoomsAndDates() {
        book("R1", date.minusDays(1), 9, 10);
        book("R2", date.plusDays(1), 9, 10);
        currentUser.set("U2");
        Booking retained = book("R1", date, 9, 10);
        admin.set(true);
        assertEquals(2, manager.cancelBookingsForUser("U1"));
        assertEquals(0, manager.cancelBookingsForUser("U1"));
        assertEquals(retained.getBookingId(), manager.listBookings().get(0).getBookingId());
        assertTrue(manager.schedule("R2", date).isEmpty());
    }

    @Test
    void deletingRoomCancelsOnlyFutureBookingsIncludingExactCutoff() {
        book("R1", date.minusDays(1), 9, 10);
        book("R1", date, 9, 11);
        book("R1", date, 11, 12);
        book("R1", date.plusDays(1), 9, 10);
        book("R2", date.plusDays(1), 9, 10);
        rooms.remove("R1");
        admin.set(true);
        assertEquals(2, manager.cancelFutureBookingsForRoom("R1", date.atTime(11, 0)));
        assertEquals(0, manager.cancelFutureBookingsForRoom("R1", date.atTime(11, 0)));
        assertEquals(3, manager.listBookings().size());
        assertEquals("R2", manager.listBookings().get(2).getRoomId());
    }

    @Test
    void ongoingRoomBookingIsRetainedAtDeletionCutoff() {
        book("R1", date, 9, 11);
        book("R1", date, 11, 12);
        admin.set(true);
        assertEquals(1, manager.cancelFutureBookingsForRoom("R1", date.atTime(10, 0)));
        assertEquals(LocalTime.of(9, 0), manager.listBookings().get(0).getStartTime());
    }

    @Test
    void cancellationRequiresAdminAndFailedChecksDoNotDeleteBookings() {
        book("R1", date, 9, 10);
        assertThrows(IllegalStateException.class, () -> manager.cancelBookingsForUser("U1"));
        assertThrows(IllegalStateException.class, () -> manager.cancelFutureBookingsForRoom("R1",
                date.atStartOfDay()));
        assertEquals(1, manager.listBookings().size());
        admin.set(true);
        assertThrows(IllegalArgumentException.class, () -> manager.cancelBookingsForUser(" "));
        assertThrows(IllegalArgumentException.class, () -> manager.cancelFutureBookingsForRoom(" ",
                date.atStartOfDay()));
        assertThrows(NullPointerException.class, () -> manager.cancelFutureBookingsForRoom("R1", null));
        assertEquals(1, manager.listBookings().size());
    }

    @Test
    void loggedOutSessionCannotQueryOrCancelEvenIfAdminSupplierIsTrue() {
        admin.set(true);
        currentUser.set(null);
        assertThrows(IllegalStateException.class, () -> manager.schedule("R1", date));
        assertThrows(IllegalStateException.class, () -> manager.isRoomAvailable("R1", date,
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        assertThrows(IllegalStateException.class, () -> manager.cancelBookingsForUser("U1"));
        assertThrows(IllegalStateException.class, () -> manager.cancelFutureBookingsForRoom("R1",
                LocalDateTime.of(date, LocalTime.NOON)));
    }
}
