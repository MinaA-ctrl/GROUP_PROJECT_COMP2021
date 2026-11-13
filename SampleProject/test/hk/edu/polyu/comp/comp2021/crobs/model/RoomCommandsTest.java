package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomCommandsTest {

    private final RoomList rooms = new RoomList();
    private final LocalDate date = LocalDate.of(2026, 11, 7);
    private final CROBS crobs = new CROBS(rooms,
            Clock.fixed(Instant.parse("2026-11-07T10:00:00Z"), ZoneOffset.UTC));
    private final String regularId;

    RoomCommandsTest() {
        crobs.login(crobs.getSuperadminId());
        regularId = crobs.addUser("Regular user").getUserId();
    }

    @Test
    void createsAndModifiesRoomsWithoutChangingIdentity() {
        Room room = crobs.addRoom("seminarRoom", 20, "projector,board,projector");
        String id = room.getRoomID();
        assertEquals(Arrays.asList("projector", "board"), room.getFeatures());
        crobs.modifyRoom(id, "capacity", "40");
        crobs.modifyRoom(id, "roomType", "computerLab");
        crobs.modifyRoom(id, "features", "computers,projector");
        assertEquals(id, room.getRoomID());
        assertEquals(40, room.getCapacity());
        assertEquals(RoomType.COMPUTER_LAB, room.getRoomType());
        assertEquals(Arrays.asList("computers", "projector"), room.getFeatures());
        assertTrue(rooms.containsRoom(id));
        assertFalse(rooms.containsRoom("missing"));
    }

    @Test
    void failedUpdatesPreserveExistingDetails() {
        Room room = crobs.addRoom("seminarRoom", 20, "projector");
        String id = room.getRoomID();
        assertThrows(IllegalArgumentException.class, () -> crobs.modifyRoom(id, "capacity", "0"));
        assertThrows(IllegalArgumentException.class, () -> crobs.modifyRoom(id, "capacity", "text"));
        assertThrows(IllegalArgumentException.class, () -> crobs.modifyRoom(id, "roomType", "unknown"));
        assertThrows(IllegalArgumentException.class, () -> crobs.modifyRoom(id, "features", "board,,screen"));
        assertThrows(IllegalArgumentException.class, () -> crobs.modifyRoom(id, "roomID", "different"));
        assertEquals(20, room.getCapacity());
        assertEquals(RoomType.SEMINAR_ROOM, room.getRoomType());
        assertEquals(Arrays.asList("projector"), room.getFeatures());
        assertEquals(id, room.getRoomID());
    }

    @Test
    void returnedListsCannotRemoveStoredRoomsOrFeaturesAndRoomOrderIsSorted() {
        Room first = crobs.addRoom("seminarRoom", 20, "projector");
        crobs.addRoom("computerLab", 30, "computers");
        List<Room> listed = crobs.listAllRooms();
        assertTrue(listed.get(0).getRoomID().compareTo(listed.get(1).getRoomID()) < 0);
        listed.clear();
        first.getFeatures().clear();
        assertEquals(2, crobs.listAllRooms().size());
        assertEquals(Arrays.asList("projector"), first.getFeatures());
    }

    @Test
    void regularUsersCanReadButCannotChangeRoomsAndLogoutIsEnforced() {
        Room room = crobs.addRoom("meetingRoom", 20, "board");
        crobs.login(regularId);
        assertEquals(room.getRoomID(), crobs.listRoom(room.getRoomID()).getRoomID());
        assertEquals(1, crobs.listAllRooms().size());
        assertThrows(IllegalStateException.class, () -> crobs.addRoom("meetingRoom", 10, "board"));
        assertThrows(IllegalStateException.class, () -> crobs.modifyRoom(room.getRoomID(), "capacity", "30"));
        assertThrows(IllegalStateException.class, () -> crobs.deleteRoom(room.getRoomID()));
        assertEquals(20, room.getCapacity());
        assertEquals(1, crobs.search(date, LocalTime.of(9, 0), LocalTime.of(10, 0), 0, "board").size());
        crobs.logout();
        assertThrows(IllegalStateException.class, crobs::listAllRooms);
        assertThrows(IllegalStateException.class, () -> crobs.listRoom(room.getRoomID()));
        assertThrows(IllegalStateException.class, () -> crobs.addRoom("meetingRoom", 10, "board"));
        assertThrows(IllegalStateException.class, () -> crobs.search(date, LocalTime.of(9, 0), LocalTime.of(10, 0), 0, "-"));
    }

    @Test
    void searchCombinesCapacityAllFeaturesAndAvailability() {
        Room available = crobs.addRoom("meetingRoom", 40, "projector,board");
        crobs.addRoom("meetingRoom", 10, "projector,board");
        crobs.addRoom("meetingRoom", 80, "projector");
        Room occupied = crobs.addRoom("lectureTheatre", 80, "projector,board");
        crobs.getBookingManager().book(occupied.getRoomID(), date, LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting");
        List<Room> result = crobs.search(date, LocalTime.of(9, 30), LocalTime.of(10, 0), 20, "board,projector");
        assertEquals(1, result.size());
        assertEquals(available.getRoomID(), result.get(0).getRoomID());
        assertEquals(2, crobs.search(date, LocalTime.of(10, 0), LocalTime.of(11, 0), 20, "board,projector").size());
        assertEquals(3, crobs.search(date, LocalTime.of(10, 0), LocalTime.of(11, 0), 20, "-").size());
    }

    @Test
    void validatesSearchEvenWhenNoRoomsMatch() {
        assertThrows(IllegalArgumentException.class, () -> crobs.search(date, LocalTime.NOON, LocalTime.NOON, 0, "-"));
        assertThrows(IllegalArgumentException.class, () -> crobs.search(date, LocalTime.of(9, 0), LocalTime.of(10, 0), -1, "-"));
        assertThrows(IllegalArgumentException.class, () -> crobs.search(date, LocalTime.of(9, 0), LocalTime.of(10, 0), 0, "board, screen"));
        assertThrows(IllegalArgumentException.class, () -> crobs.search(date, LocalTime.of(9, 0), LocalTime.of(10, 0), 0, "board,"));
    }

    @Test
    void deletingRoomCancelsItsFutureBookingsAndRejectsFurtherBookings() {
        Room removed = crobs.addRoom("meetingRoom", 20, "board");
        Room retained = crobs.addRoom("meetingRoom", 20, "board");
        BookingManager bookings = crobs.getBookingManager();
        bookings.book(removed.getRoomID(), date.minusDays(1), LocalTime.of(9, 0), LocalTime.of(10, 0), "Past");
        bookings.book(removed.getRoomID(), date, LocalTime.of(9, 0), LocalTime.of(11, 0), "Ongoing");
        bookings.book(removed.getRoomID(), date, LocalTime.of(11, 0), LocalTime.NOON, "Future");
        bookings.book(retained.getRoomID(), date, LocalTime.of(11, 0), LocalTime.NOON, "Other room");
        crobs.login(regularId);
        assertThrows(IllegalStateException.class, () -> crobs.deleteRoom(removed.getRoomID()));
        crobs.login(crobs.getSuperadminId());
        assertEquals(4, bookings.listBookings().size());
        assertThrows(IllegalArgumentException.class, () -> crobs.deleteRoom("missing"));
        assertEquals(4, bookings.listBookings().size());
        crobs.deleteRoom(removed.getRoomID());
        assertFalse(rooms.containsRoom(removed.getRoomID()));
        assertTrue(rooms.containsRoom(retained.getRoomID()));
        assertEquals(3, bookings.listBookings().size());
        assertFalse(bookings.listBookings().stream().anyMatch(booking -> "Future".equals(booking.getPurpose())));
        assertThrows(IllegalArgumentException.class, () -> bookings.book(removed.getRoomID(), date.plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(10, 0), "Deleted room"));
    }

    @Test
    void deletingAnEarlierRoomDoesNotReuseExistingOrDeletedIds() {
        Room first = crobs.addRoom("seminarRoom", 20, "projector");
        Room second = crobs.addRoom("seminarRoom", 20, "projector");
        crobs.deleteRoom(first.getRoomID());
        Room replacement = crobs.addRoom("seminarRoom", 20, "projector");
        assertNotEquals(first.getRoomID(), replacement.getRoomID());
        assertNotEquals(second.getRoomID(), replacement.getRoomID());
    }

    @Test
    void deletingARoomAfterTypeChangeDoesNotReuseIdsOfTheNewType() {
        Room changed = crobs.addRoom("seminarRoom", 20, "projector");
        Room existingLab = crobs.addRoom("computerLab", 20, "computers");
        crobs.modifyRoom(changed.getRoomID(), "roomType", "computerLab");
        crobs.deleteRoom(changed.getRoomID());
        Room newLab = crobs.addRoom("computerLab", 20, "computers");
        assertNotEquals(existingLab.getRoomID(), newLab.getRoomID());
    }

    @Test
    void acceptsEmptyFeaturesAndRejectsInvalidCreation() {
        assertTrue(crobs.addRoom("meetingRoom", 10, "-").getFeatures().isEmpty());
        assertTrue(crobs.addRoom("meetingRoom", 10, "").getFeatures().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> crobs.addRoom("unknown", 10, "board"));
        assertThrows(IllegalArgumentException.class, () -> crobs.addRoom("meetingRoom", -1, "board"));
        assertThrows(IllegalArgumentException.class, () -> crobs.addRoom("meetingRoom", 10, "board, screen"));
        assertEquals(2, crobs.listAllRooms().size());
    }
}
