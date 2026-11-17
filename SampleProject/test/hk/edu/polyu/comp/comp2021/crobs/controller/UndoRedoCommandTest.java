package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UndoRedoCommandTest {

    private final CROBS crobs = new CROBS();
    private final CommandProcessor processor = new CommandProcessor(crobs);

    @Test
    void undoAndRedoCommandsReportWhatChanged() {
        assertTrue(run("undo").contains("Error: Log in before using undo"));
        run("login sd00000000");
        String roomId = run("addRoom meetingRoom 12 tv").replace("Room created: ", "").trim();
        String booking = run("book " + roomId + " 2026-12-01 09:00 10:00 Planning meeting");
        assertTrue(booking.contains("Booking created: B1"));
        assertTrue(run("undo").contains("Undone: book " + roomId + " 2026-12-01 09:00 10:00"));
        assertTrue(run("listBookings").contains("No bookings."));
        assertTrue(run("redo").contains("Redone: book"));
        assertTrue(run("listBookings").contains("Planning meeting"));
        assertTrue(run("undo extra").contains("Error: Usage: undo"));
        assertTrue(run("redo extra").contains("Error: Usage: redo"));
        assertTrue(run("redo").contains("Error: Nothing to redo"));
        run("undo");
        run("undo");
        assertTrue(run("listAllRooms").contains("No rooms found."));
        assertTrue(run("undo").contains("Error: Nothing to undo"));
    }

    @Test
    void processorReportsQuitAndBlankLines() {
        StringWriter buffer = new StringWriter();
        assertFalse(processor.execute("quit", new PrintWriter(buffer)));
        assertEquals("Goodbye.", buffer.toString().trim());
        assertTrue(run("   ").contains("Error: Enter a command"));
        assertTrue(run("logout now").contains("Error: Usage: logout"));
    }

    private String run(String command) {
        StringWriter buffer = new StringWriter();
        assertTrue(processor.execute(command, new PrintWriter(buffer)));
        return buffer.toString();
    }
}
