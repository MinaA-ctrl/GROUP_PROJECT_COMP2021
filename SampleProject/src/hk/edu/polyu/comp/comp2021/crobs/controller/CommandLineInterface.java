package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.logging.OperationLogger;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.BookingManager;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.ScheduleEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DateTimeException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Reads commands, records each line, and dispatches room and booking operations. */
public final class CommandLineInterface {

    private final BookingManager bookings;
    private final CROBS crobs;

    public CommandLineInterface(CROBS crobs) {
        this.crobs = Objects.requireNonNull(crobs, "crobs");
        this.bookings = crobs.getBookingManager();
    }

    /**
     * Runs until quit or end of input. Input and output remain caller-owned;
     * the application's try-with-resources closes the logger. Logging failures
     * stop the loop before the affected command is executed.
     */
    public void run(Reader input, PrintWriter output, OperationLogger logger) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(logger, "logger");
        BufferedReader reader = input instanceof BufferedReader
                ? (BufferedReader) input : new BufferedReader(input);
        output.println("Crobs. Type quit to exit.");
        while (true) {
            output.print("crobs> ");
            output.flush();
            String rawLine = reader.readLine();
            if (rawLine == null) {
                return;
            }
            logger.log(rawLine);
            try {
                if (!execute(rawLine, output)) {
                    return;
                }
            } catch (DateTimeException invalidDateOrTime) {
                output.println("Error: use YYYY-MM-DD for dates and HH:mm for times.");
            } catch (IllegalArgumentException | IllegalStateException rejectedCommand) {
                output.println("Error: " + rejectedCommand.getMessage());
            }
        }
    }

    private boolean execute(String rawLine, PrintWriter output) {
        String line = rawLine.trim();
        if (line.isEmpty()) {
            throw new IllegalArgumentException("Enter a command");
        }
        String command = line.split("\\s+", 2)[0];
        String[] args;
        switch (command) {
            case "quit":
                requireArguments(line.split("\\s+"), 1, "quit");
                output.println("Goodbye.");
                output.flush();
                return false;
            case "book":
                args = line.split("\\s+", 6);
                requireArguments(args, 6, "book roomId date startTime endTime purpose");
                Booking created = bookings.book(args[1], LocalDate.parse(args[2]),
                        LocalTime.parse(args[3]), LocalTime.parse(args[4]), args[5]);
                output.println("Booking created: " + created.getBookingId());
                break;
            case "modifyBooking":
                args = line.split("\\s+", 7);
                requireArguments(args, 7, "modifyBooking bookingID roomId date startTime endTime purpose");
                Booking modified = bookings.modifyBooking(args[1], args[2], LocalDate.parse(args[3]),
                        LocalTime.parse(args[4]), LocalTime.parse(args[5]), args[6]);
                output.println("Booking modified: " + modified.getBookingId());
                break;
            case "deleteBooking":
                args = line.split("\\s+");
                requireArguments(args, 2, "deleteBooking bookingID");
                bookings.deleteBooking(args[1]);
                output.println("Booking deleted: " + args[1]);
                break;
            case "listBookings":
                requireArguments(line.split("\\s+"), 1, "listBookings");
                printBookings(output);
                break;
            case "schedule":
                args = line.split("\\s+");
                requireArguments(args, 3, "schedule roomId weekStartDate");
                printSchedule(args[1], LocalDate.parse(args[2]), output);
                break;
            default:
                executeRoomCommand(command, line, output);
        }
        output.flush();
        return true;
    }

    private void executeRoomCommand(String command, String line, PrintWriter output) {
        String[] args = line.split("\\s+");
        switch (command) {
            case "addRoom":
                requireArguments(args, 4, "addRoom roomType capacity features");
                Room added = crobs.addRoom(args[1], parseInteger(args[2], "Capacity"), args[3]);
                output.println("Room created: " + added.getRoomID());
                break;
            case "modifyRoom":
                requireArguments(args, 4, "modifyRoom roomID field newValue");
                Room modified = crobs.modifyRoom(args[1], args[2], args[3]);
                output.println("Room modified: " + modified.getRoomID());
                break;
            case "deleteRoom":
                requireArguments(args, 2, "deleteRoom roomID");
                crobs.deleteRoom(args[1]);
                output.println("Room deleted: " + args[1]);
                break;
            case "listRoom":
                requireArguments(args, 2, "listRoom roomID");
                printRooms(Collections.singletonList(crobs.listRoom(args[1])), output);
                break;
            case "listAllRooms":
                requireArguments(args, 1, "listAllRooms");
                printRooms(crobs.listAllRooms(), output);
                break;
            case "search":
                requireArguments(args, 6, "search date startTime endTime minCapacity features");
                printRooms(crobs.search(LocalDate.parse(args[1]), LocalTime.parse(args[2]),
                        LocalTime.parse(args[3]), parseInteger(args[4], "Minimum capacity"), args[5]), output);
                break;
            default:
                throw new IllegalArgumentException("Unknown command: " + command);
        }
    }

    private static int parseInteger(String value, String label) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException invalidNumber) {
            throw new IllegalArgumentException(label + " must be an integer");
        }
    }

    private static void printRooms(List<Room> rooms, PrintWriter output) {
        if (rooms.isEmpty()) {
            output.println("No rooms found.");
            return;
        }
        output.println("Room ID\tRoom type\tCapacity\tFeatures");
        for (Room room : rooms) {
            output.println(room.getRoomID() + "\t" + room.getRoomType() + "\t"
                    + room.getCapacity() + "\t" + String.join(",", room.getFeatures()));
        }
    }

    private void printBookings(PrintWriter output) {
        List<Booking> result = bookings.listBookings();
        if (result.isEmpty()) {
            output.println("No bookings.");
            return;
        }
        output.println("Booking ID\tRoom ID\tDate\tStart\tEnd\tPurpose");
        for (Booking booking : result) {
            output.println(booking.getBookingId() + "\t" + booking.getRoomId() + "\t"
                    + booking.getDate() + "\t" + booking.getStartTime() + "\t"
                    + booking.getEndTime() + "\t" + booking.getPurpose());
        }
    }

    private void printSchedule(String roomId, LocalDate weekStart, PrintWriter output) {
        List<ScheduleEntry> result = bookings.schedule(roomId, weekStart);
        if (result.isEmpty()) {
            output.println("No bookings in this seven-day period.");
            return;
        }
        output.println("Booking ID\tRoom ID\tDate\tStart\tEnd\tOwner\tPurpose");
        for (ScheduleEntry entry : result) {
            String owner = entry.getOwnerUserId() == null ? "hidden" : entry.getOwnerUserId();
            output.println(entry.getBookingId() + "\t" + entry.getRoomId() + "\t"
                    + entry.getDate() + "\t" + entry.getStartTime() + "\t"
                    + entry.getEndTime() + "\t" + owner + "\t" + entry.getPurpose());
        }
    }

    private static void requireArguments(String[] args, int count, String usage) {
        if (args.length != count) {
            throw new IllegalArgumentException("Usage: " + usage);
        }
    }
}
