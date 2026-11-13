package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.logging.OperationLogger;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomCommandInterfaceTest {

    private final CROBS crobs = new CROBS();
    private final Room room;

    RoomCommandInterfaceTest() {
        crobs.login(crobs.getSuperadminId());
        room = crobs.addRoom("meetingRoom", 20, "board");
    }

    @Test
    void executesRoomCommandsAndRecordsThemOnce() throws IOException {
        Path directory = Files.createTempDirectory("crobs-room-cli-");
        String commands = "addRoom computerLab 30 computers,projector\n"
                + "modifyRoom " + room.getRoomID() + " capacity 40\n"
                + "listRoom " + room.getRoomID() + "\nlistAllRooms\n"
                + "search 2026-11-10 09:00 10:00 35 board\n"
                + "deleteRoom " + room.getRoomID() + "\nquit\n";
        String output = run(commands, directory);
        assertTrue(output.contains("Room created:"));
        assertTrue(output.contains("Room modified: " + room.getRoomID()));
        assertTrue(output.contains("meetingRoom\t40\tboard"));
        assertTrue(output.contains("Room deleted: " + room.getRoomID()));
        assertEquals(1, crobs.listAllRooms().size());
        assertFalse(crobs.listAllRooms().stream().anyMatch(item -> item.getRoomID().equals(room.getRoomID())));
        assertEquals(7, Files.readAllLines(directory.resolve("log.txt"), StandardCharsets.UTF_8).size());
    }

    @Test
    void searchOutputExcludesOccupiedRooms() throws IOException {
        Room free = crobs.addRoom("meetingRoom", 20, "board");
        crobs.getBookingManager().book(room.getRoomID(), LocalDate.of(2026, 11, 10),
                LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting");
        String output = run("search 2026-11-10 09:30 10:00 20 board\nquit\n",
                Files.createTempDirectory("crobs-room-search-cli-"));
        assertTrue(output.contains(free.getRoomID()));
        assertFalse(output.contains(room.getRoomID()));
    }

    @Test
    void rejectsUnauthorizedAndMalformedRoomCommandsThenContinues() throws IOException {
        crobs.login(crobs.addUser("Regular user").getUserId());
        String output = run("addRoom meetingRoom 10 board\n"
                + "modifyRoom " + room.getRoomID() + " capacity 30\n"
                + "deleteRoom " + room.getRoomID() + "\n"
                + "addRoom meetingRoom invalid board\nsearch\n"
                + "listAllRooms\nquit\n", Files.createTempDirectory("crobs-room-errors-cli-"));
        assertTrue(output.contains("Admin privileges are required"));
        assertTrue(output.contains("Capacity must be an integer"));
        assertTrue(output.contains("Usage: search"));
        assertTrue(output.contains(room.getRoomID()));
        assertTrue(output.contains("Goodbye."));
        assertEquals(20, room.getCapacity());
        assertEquals(1, crobs.listAllRooms().size());
    }

    @Test
    void emptyFeatureConventionWorksAndInvalidDateRangeDoesNotCrashLoop() throws IOException {
        String output = run("modifyRoom " + room.getRoomID() + " features -\n"
                + "search 2026-11-10 09:00 10:00 0 -\n"
                + "schedule " + room.getRoomID() + " +999999999-12-31\nquit\n",
                Files.createTempDirectory("crobs-room-empty-cli-"));
        assertTrue(room.getFeatures().isEmpty());
        assertTrue(output.contains(room.getRoomID()));
        assertTrue(output.contains("Error: use YYYY-MM-DD"));
        assertTrue(output.contains("Goodbye."));
    }

    private String run(String commands, Path directory) throws IOException {
        StringWriter output = new StringWriter();
        try (OperationLogger logger = new OperationLogger(directory.resolve("log.html"), directory.resolve("log.txt"))) {
            new CommandLineInterface(crobs).run(new StringReader(commands), new PrintWriter(output), logger);
        }
        return output.toString();
    }
}
