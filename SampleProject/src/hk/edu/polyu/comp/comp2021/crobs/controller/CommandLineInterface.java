package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.logging.OperationLogger;
import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.BookingManager;
import hk.edu.polyu.comp.comp2021.crobs.model.ScheduleEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

/** Reads commands, records each line, and dispatches booking operations. */
public final class CommandLineInterface {

    private final BookingManager bookings;

    public CommandLineInterface(BookingManager bookings) {
        this.bookings = Objects.requireNonNull(bookings, "bookings");
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
            } catch (DateTimeParseException invalidDateOrTime) {
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
                throw new IllegalArgumentException("Unknown command: " + command);
        }
        output.flush();
        return true;
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
