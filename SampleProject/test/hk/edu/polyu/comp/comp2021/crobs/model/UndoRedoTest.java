package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UndoRedoTest {

    private static final LocalDate DAY = LocalDate.of(2026, 12, 1);
    private static final LocalTime NINE = LocalTime.of(9, 0);
    private static final LocalTime TEN = LocalTime.of(10, 0);

    private final RoomList rooms = new RoomList();
    private final CROBS crobs = new CROBS(rooms,
            Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC));

    UndoRedoTest() {
        crobs.login(crobs.getSuperadminId());
    }

    @Test
    void undoAndRedoAddRoomKeepTheSameRoomAndNeverReuseIds() {
        Room room = crobs.addRoom("meetingRoom", 10, "tv");
        assertTrue(crobs.canUndo());
        assertFalse(crobs.canRedo());
        assertEquals("addRoom meetingRoom 10 tv", crobs.undo());
        assertFalse(rooms.containsRoom(room.getRoomID()));
        assertFalse(crobs.canUndo());
        assertTrue(crobs.canRedo());
        assertEquals("addRoom meetingRoom 10 tv", crobs.redo());
        assertEquals(10, crobs.listRoom(room.getRoomID()).getCapacity());
        assertEquals(Collections.singletonList("tv"), crobs.listRoom(room.getRoomID()).getFeatures());
        crobs.undo();
        Room next = crobs.addRoom("meetingRoom", 10, "tv");
        assertNotEquals(room.getRoomID(), next.getRoomID());
        assertFalse(crobs.canRedo());
    }

    @Test
    void undoModifyRoomRestoresOldValuesAndRedoReappliesThem() {
        String id = crobs.addRoom("seminarRoom", 20, "projector").getRoomID();
        crobs.modifyRoom(id, "capacity", "45");
        crobs.modifyRoom(id, "features", "board");
        crobs.undo();
        crobs.undo();
        assertEquals(20, crobs.listRoom(id).getCapacity());
        assertEquals(Collections.singletonList("projector"), crobs.listRoom(id).getFeatures());
        crobs.redo();
        assertEquals(45, crobs.listRoom(id).getCapacity());
        crobs.redo();
        assertEquals(Collections.singletonList("board"), crobs.listRoom(id).getFeatures());
        assertThrows(IllegalStateException.class, crobs::redo);
    }

    @Test
    void undoDeleteRoomRestoresTheRoomAndItsCancelledBookings() {
        String id = crobs.addRoom("lectureTheatre", 100, "-").getRoomID();
        Booking booking = crobs.book(id, DAY, NINE, TEN, "Lecture");
        crobs.deleteRoom(id);
        assertTrue(crobs.listBookings().isEmpty());
        assertEquals("deleteRoom " + id, crobs.undo());
        assertTrue(rooms.containsRoom(id));
        assertEquals(booking.getBookingId(), crobs.listBookings().get(0).getBookingId());
        crobs.redo();
        assertFalse(rooms.containsRoom(id));
        assertTrue(crobs.listBookings().isEmpty());
    }

    @Test
    void undoBookingCommandsAndBookingIdsAreNotReused() {
        String id = crobs.addRoom("computerLab", 30, "pc").getRoomID();
        Booking first = crobs.book(id, DAY, NINE, TEN, "Lab");
        crobs.modifyBooking(first.getBookingId(), id, DAY, TEN, LocalTime.of(11, 0), "Moved lab");
        assertEquals("modifyBooking " + first.getBookingId(), crobs.undo());
        assertEquals(NINE, crobs.listBookings().get(0).getStartTime());
        assertEquals("Lab", crobs.listBookings().get(0).getPurpose());
        crobs.deleteBooking(first.getBookingId());
        assertTrue(crobs.listBookings().isEmpty());
        assertEquals("deleteBooking " + first.getBookingId(), crobs.undo());
        assertEquals(1, crobs.listBookings().size());
        crobs.undo();
        assertTrue(crobs.listBookings().isEmpty());
        Booking second = crobs.book(id, DAY, NINE, TEN, "Lab again");
        assertNotEquals(first.getBookingId(), second.getBookingId());
    }

    @Test
    void undoUserCommandsRestoresAccountsRolesAndBookings() {
        String roomId = crobs.addRoom("meetingRoom", 8, "-").getRoomID();
        User user = crobs.addUser("Alex");
        crobs.changeRole(user.getUserId(), "admin");
        crobs.undo();
        crobs.login(user.getUserId());
        assertEquals(UserType.REGULAR, crobs.getCurrentUser().get().getRole());
        crobs.book(roomId, DAY, NINE, TEN, "Study");
        crobs.login(crobs.getSuperadminId());
        crobs.removeUser(user.getUserId());
        assertThrows(IllegalArgumentException.class, () -> crobs.changeRole(user.getUserId(), "admin"));
        assertEquals("removeUser " + user.getUserId(), crobs.undo());
        assertEquals(1, crobs.schedule(roomId, DAY).size());
        assertEquals(user.getUserId(), crobs.schedule(roomId, DAY).get(0).getOwnerUserId());
        crobs.redo();
        assertTrue(crobs.schedule(roomId, DAY).isEmpty());
    }

    @Test
    void undoAddUserRemovesTheAccount() {
        User user = crobs.addUser("Sam");
        assertEquals("addUser Sam", crobs.undo());
        assertThrows(IllegalArgumentException.class, () -> crobs.login(user.getUserId()));
    }

    @Test
    void failedCommandsAndReadOnlyCommandsAreNotRecorded() {
        assertThrows(IllegalArgumentException.class, () -> crobs.addRoom("unknown", 10, "-"));
        assertThrows(IllegalArgumentException.class, () -> crobs.deleteRoom("missing"));
        String id = crobs.addRoom("meetingRoom", 10, "-").getRoomID();
        crobs.listAllRooms();
        crobs.listRoom(id);
        crobs.search(DAY, NINE, TEN, 0, "-");
        crobs.schedule(id, DAY);
        crobs.listBookings();
        crobs.undo();
        assertFalse(crobs.canUndo());
        assertThrows(IllegalStateException.class, crobs::undo);
    }

    @Test
    void loginAndLogoutClearTheHistoryAndUndoNeedsALogin() {
        crobs.addRoom("meetingRoom", 10, "-");
        crobs.login(crobs.getSuperadminId());
        assertFalse(crobs.canUndo());
        crobs.addRoom("meetingRoom", 10, "-");
        crobs.undo();
        crobs.logout();
        assertFalse(crobs.canRedo());
        assertFalse(crobs.getCurrentUser().isPresent());
        IllegalStateException undo = assertThrows(IllegalStateException.class, crobs::undo);
        assertEquals("Log in before using undo", undo.getMessage());
        IllegalStateException redo = assertThrows(IllegalStateException.class, crobs::redo);
        assertEquals("Log in before using redo", redo.getMessage());
    }

    @Test
    void snapshotsAreIndependentOfLaterChanges() {
        String id = crobs.addRoom("seminarRoom", 20, "a,b").getRoomID();
        crobs.modifyRoom(id, "roomType", "computerLab");
        crobs.modifyRoom(id, "capacity", "30");
        crobs.undo();
        crobs.undo();
        assertEquals(RoomType.SEMINAR_ROOM, crobs.listRoom(id).getRoomType());
        assertEquals(Arrays.asList("a", "b"), crobs.listRoom(id).getFeatures());
        assertEquals(20, crobs.listRoom(id).getCapacity());
    }

    @Test
    void remainingValidationPathsAreReported() {
        Room plain = new Room(5, RoomType.MEETING_ROOM);
        assertTrue(plain.getFeatures().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new Room(5, null));
        assertThrows(IllegalArgumentException.class, () -> plain.modifyField("capacity", null));
        assertTrue(Room.parseFeatures(null).isEmpty());
        String id = crobs.addRoom("meetingRoom", 10, "-").getRoomID();
        assertThrows(IllegalArgumentException.class, () -> rooms.modifyRoom(id, null, "5"));
        assertThrows(IllegalArgumentException.class, () -> crobs.addUser(null));
        assertThrows(IllegalArgumentException.class, () -> crobs.addUser("  "));
        assertThrows(IllegalArgumentException.class, () -> crobs.book(id, DAY, NINE, TEN, " "));
        BookingManager standalone = new BookingManager(() -> "u1", room -> true);
        standalone.book("r1", DAY, NINE, TEN, "Talk");
        assertThrows(IllegalStateException.class, () -> standalone.cancelBookingsForUser("u1"));
        BookingManager blankUser = new BookingManager(() -> " ", room -> true);
        assertThrows(IllegalStateException.class, blankUser::listBookings);
        RoomType.resetRoomTypeCount();
        assertEquals("lt00000000", new Room(5, RoomType.LECTURE_THEATRE).getRoomID());
    }
}
