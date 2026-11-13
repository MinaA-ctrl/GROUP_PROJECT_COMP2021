package hk.edu.polyu.comp.comp2021.crobs.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserCommandsTest {

    private final CROBS crobs = new CROBS();

    @Test
    void startsLoggedOutWithOneSuperadminAndRejectsUnknownLogin() {
        assertThrows(IllegalStateException.class, crobs::listAllRooms);
        assertThrows(IllegalStateException.class, crobs::logout);
        assertThrows(IllegalArgumentException.class, () -> crobs.login("missing"));
        assertEquals(UserType.SUPERADMIN, crobs.login(crobs.getSuperadminId()).getRole());
        assertTrue(crobs.listAllRooms().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> crobs.login("missing"));
        assertEquals(UserType.REGULAR, crobs.addUser("Alex").getRole());
        crobs.logout();
        assertThrows(IllegalStateException.class, () -> crobs.addRoom("meetingRoom", 12, "-"));
    }

    @Test
    void userIdsRemainUniqueAndAreNeverReusedAfterRemoval() {
        crobs.login(crobs.getSuperadminId());
        User first = crobs.addUser("First");
        User second = crobs.addUser("Second");
        crobs.removeUser(first.getUserId());
        User third = crobs.addUser("Third");
        assertNotEquals(first.getUserId(), third.getUserId());
        assertNotEquals(second.getUserId(), third.getUserId());
        assertThrows(IllegalArgumentException.class, () -> crobs.login(first.getUserId()));
        assertEquals("Second", crobs.login(second.getUserId()).getName());
    }

    @Test
    void rolesProtectAdminCommandsAndTheSuperadminAccount() {
        assertThrows(IllegalStateException.class, () -> crobs.addUser("No session"));
        crobs.login(crobs.getSuperadminId());
        assertThrows(IllegalArgumentException.class, () -> crobs.addUser(" "));
        User admin = crobs.addUser("Admin candidate");
        User regular = crobs.addUser("Regular user");
        assertThrows(IllegalArgumentException.class,
                () -> crobs.changeRole(regular.getUserId(), "unknown"));
        assertThrows(IllegalArgumentException.class,
                () -> crobs.changeRole(regular.getUserId(), "superadmin"));
        assertThrows(IllegalArgumentException.class,
                () -> crobs.changeRole(crobs.getSuperadminId(), "regular"));
        assertThrows(IllegalArgumentException.class, () -> crobs.removeUser(crobs.getSuperadminId()));
        crobs.changeRole(admin.getUserId(), "ADMIN");
        crobs.login(regular.getUserId());
        assertThrows(IllegalStateException.class, () -> crobs.addUser("Other"));
        assertThrows(IllegalStateException.class, () -> crobs.removeUser(regular.getUserId()));
        assertThrows(IllegalStateException.class, () -> crobs.changeRole(regular.getUserId(), "admin"));
        crobs.login(admin.getUserId());
        assertThrows(IllegalStateException.class, () -> crobs.changeRole(regular.getUserId(), "admin"));
        assertThrows(IllegalArgumentException.class, () -> crobs.removeUser(admin.getUserId()));
        assertEquals(UserType.REGULAR, crobs.addUser("Added by admin").getRole());
        crobs.removeUser(regular.getUserId());
        assertThrows(IllegalArgumentException.class, () -> crobs.login(regular.getUserId()));
    }

    @Test
    void roleChangesPreserveBookingsAndUpdateRoomPermissions() {
        crobs.login(crobs.getSuperadminId());
        String roomId = crobs.addRoom("meetingRoom", 12, "-").getRoomID();
        String ownerId = crobs.addUser("Booking owner").getUserId();
        crobs.login(ownerId);
        Booking booking = book(roomId, 9);
        crobs.login(crobs.getSuperadminId());
        User promoted = crobs.changeRole(ownerId, "admin");
        assertEquals(ownerId, promoted.getUserId());
        assertEquals("Booking owner", promoted.getName());
        assertEquals(UserType.ADMIN, crobs.login(ownerId).getRole());
        crobs.addRoom("computerLab", 20, "computers");
        assertEquals(booking.getBookingId(), crobs.getBookingManager().listBookings().get(0).getBookingId());
        crobs.login(crobs.getSuperadminId());
        crobs.changeRole(ownerId, "regular");
        crobs.login(ownerId);
        assertThrows(IllegalStateException.class, () -> crobs.addRoom("meetingRoom", 12, "-"));
        assertEquals(ownerId, crobs.getBookingManager().listBookings().get(0).getOwnerUserId());
    }

    @Test
    void removalCancelsOnlyTheTargetUsersBookings() {
        crobs.login(crobs.getSuperadminId());
        String roomId = crobs.addRoom("meetingRoom", 12, "-").getRoomID();
        String firstId = crobs.addUser("First").getUserId();
        String secondId = crobs.addUser("Second").getUserId();
        crobs.login(firstId);
        book(roomId, 9);
        crobs.login(secondId);
        Booking kept = book(roomId, 10);
        crobs.login(crobs.getSuperadminId());
        assertThrows(IllegalArgumentException.class, () -> crobs.removeUser("missing"));
        assertFalse(crobs.getBookingManager().isRoomAvailable(roomId, LocalDate.of(2026, 12, 1),
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        crobs.removeUser(firstId);
        assertTrue(crobs.getBookingManager().isRoomAvailable(roomId, LocalDate.of(2026, 12, 1),
                LocalTime.of(9, 0), LocalTime.of(10, 0)));
        List<ScheduleEntry> remaining = crobs.getBookingManager().schedule(roomId, LocalDate.of(2026, 12, 1));
        assertEquals(1, remaining.size());
        assertEquals(kept.getBookingId(), remaining.get(0).getBookingId());
        assertThrows(IllegalArgumentException.class, () -> crobs.login(firstId));
    }

    private Booking book(String roomId, int hour) {
        return crobs.getBookingManager().book(roomId, LocalDate.of(2026, 12, 1),
                LocalTime.of(hour, 0), LocalTime.of(hour + 1, 0), "Meeting");
    }
}
