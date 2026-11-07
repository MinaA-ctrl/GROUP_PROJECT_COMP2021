package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.logging.OperationLogger;
import hk.edu.polyu.comp.comp2021.crobs.model.BookingManager;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomList;
import hk.edu.polyu.comp.comp2021.crobs.model.RoomType;
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
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandLineInterfaceTest {

    private final RoomList rooms = new RoomList();
    private final String roomId = rooms.addRoom(20, RoomType.MEETING_ROOM, "projector").getRoomID();
    private final AtomicReference<String> currentUser = new AtomicReference<>("U1");
    private final AtomicBoolean admin = new AtomicBoolean(false);
    private final CROBS crobs = new CROBS(rooms, currentUser::get, admin::get);
    private final BookingManager bookings = crobs.getBookingManager();

    @Test
    void executesBookingLifecycleAndLogsEachRawCommandOnce() throws IOException {
        String create = "  book " + roomId + " 2026-11-10 09:00 10:00 Team project meeting  ";
        String modify = "modifyBooking B1 " + roomId + " 2026-11-10 10:00 11:00 Updated meeting purpose";
        String[] commands = {create, modify, "listBookings", "deleteBooking B1", "listBookings", "quit"};
        Path directory = Files.createTempDirectory("crobs-cli-lifecycle-");
        String output = run(String.join("\n", commands) + "\n", directory);
        assertTrue(output.contains("Booking created: B1"));
        assertTrue(output.contains("Booking modified: B1"));
        assertTrue(output.contains("Updated meeting purpose"));
        assertTrue(output.contains("Booking deleted: B1"));
        assertTrue(output.contains("No bookings."));
        assertTrue(bookings.listBookings().isEmpty());
        assertEquals(Arrays.asList(commands), Files.readAllLines(directory.resolve("log.txt"), StandardCharsets.UTF_8));
        String html = new String(Files.readAllBytes(directory.resolve("log.html")), StandardCharsets.UTF_8);
        assertTrue(html.contains("<tr><td>6</td><td>quit</td></tr>"));
        assertTrue(html.endsWith("</html>\n"));
    }

    @Test
    void malformedAndRejectedCommandsDoNotEndSession() throws IOException {
        String commands = "unknown\nbook\nbook " + roomId + " invalid-date 09:00 10:00 Meeting\n"
                + "book " + roomId + " 2026-11-10 09:00 10:00 Meeting\n"
                + "book " + roomId + " 2026-11-10 09:30 10:30 Conflict\n"
                + "quit extra\nlistBookings\nquit\nnot reached\n";
        Path directory = Files.createTempDirectory("crobs-cli-errors-");
        String output = run(commands, directory);
        assertTrue(output.contains("Error: Unknown command: unknown"));
        assertTrue(output.contains("Error: Usage: book"));
        assertTrue(output.contains("Error: use YYYY-MM-DD"));
        assertTrue(output.contains("Room is already booked"));
        assertTrue(output.contains("Error: Usage: quit"));
        assertEquals(1, bookings.listBookings().size());
        assertEquals(8, Files.readAllLines(directory.resolve("log.txt"), StandardCharsets.UTF_8).size());
        assertFalse(output.contains("not reached"));
    }

    @Test
    void schedulePrintsOnlyOwnerIdsPermittedByLiveSession() throws IOException {
        bookings.book(roomId, LocalDate.of(2026, 11, 10), LocalTime.of(9, 0), LocalTime.of(10, 0), "Meeting");
        currentUser.set("U2");
        String commands = "schedule " + roomId + " 2026-11-10\nquit\n";
        String regularOutput = run(commands, Files.createTempDirectory("crobs-cli-private-"));
        assertTrue(regularOutput.contains("hidden"));
        assertFalse(regularOutput.contains("U1"));
        admin.set(true);
        String adminOutput = run(commands, Files.createTempDirectory("crobs-cli-admin-"));
        assertTrue(adminOutput.contains("U1"));
    }

    @Test
    void loggedOutSessionCannotCreateBookingsButCanQuit() throws IOException {
        currentUser.set(null);
        Path directory = Files.createTempDirectory("crobs-cli-loggedout-");
        String output = run("book " + roomId + " 2026-11-10 09:00 10:00 Meeting\nquit\n", directory);
        assertTrue(output.contains("Log in before managing bookings"));
        assertTrue(output.contains("Goodbye."));
        currentUser.set("U1");
        assertTrue(bookings.listBookings().isEmpty());
    }

    @Test
    void loggingFailureStopsBeforeCommandChangesModel() throws IOException {
        Path directory = Files.createTempDirectory("crobs-cli-log-failure-");
        OperationLogger logger = new OperationLogger(directory.resolve("log.html"), directory.resolve("log.txt"));
        logger.close();
        assertThrows(IOException.class, () -> new CommandLineInterface(bookings).run(
                new StringReader("book " + roomId + " 2026-11-10 09:00 10:00 Meeting\n"),
                new PrintWriter(new StringWriter()), logger));
        assertTrue(bookings.listBookings().isEmpty());
    }

    private String run(String commands, Path directory) throws IOException {
        StringWriter output = new StringWriter();
        try (OperationLogger logger = new OperationLogger(directory.resolve("log.html"), directory.resolve("log.txt"))) {
            new CommandLineInterface(bookings).run(new StringReader(commands), new PrintWriter(output), logger);
        }
        return output.toString();
    }
}
