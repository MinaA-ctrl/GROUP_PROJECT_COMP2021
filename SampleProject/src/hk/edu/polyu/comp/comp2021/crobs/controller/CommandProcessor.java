package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.model.Booking;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.model.Room;
import hk.edu.polyu.comp.comp2021.crobs.model.ScheduleEntry;
import hk.edu.polyu.comp.comp2021.crobs.model.User;

import java.io.PrintWriter;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Parses one command line, calls the model and prints the result as text. Both the
 * command-line interface and the GUI send every command through this class, so the
 * two front ends always behave the same way.
 */
public final class CommandProcessor {

    private final CROBS crobs;

    /**
     * Creates a processor for one CROBS session.
     *
     * @param crobs the model to operate on
     */
    public CommandProcessor(CROBS crobs) {
        this.crobs = Objects.requireNonNull(crobs, "crobs");
    }

    /**
     * Executes one command line. Ill-formed or rejected commands print an error message
     * and leave the model unchanged.
     *
     * @param rawLine the command as typed by the user
     * @param output where results and error messages are printed
     * @return false if the command was quit, true otherwise
     */
    public boolean execute(String rawLine, PrintWriter output) {
        try {
            return dispatch(rawLine, output);
        } catch (DateTimeException invalidDateOrTime) {
            output.println("Error: use YYYY-MM-DD for dates and HH:mm for times.");
        } catch (IllegalArgumentException | IllegalStateException rejectedCommand) {
            output.println("Error: " + rejectedCommand.getMessage());
        } finally {
            output.flush();
        }
        return true;
    }

    private boolean dispatch(String rawLine, PrintWriter output) {
        String line = rawLine.trim();
        if (line.isEmpty()) {
            throw new IllegalArgumentException("Enter a command");
        }
        String command = line.split("\\s+", 2)[0];
        switch (command) {
            case "quit":
                requireArguments(line.split("\\s+"), 1, "quit");
                output.println("Goodbye.");
                return false;
            case "addUser":
            case "removeUser":
            case "changeRole":
            case "login":
            case "logout":
                executeUserCommand(command, line, output);
                break;
            case "book":
            case "modifyBooking":
            case "deleteBooking":
            case "listBookings":
            case "schedule":
                executeBookingCommand(command, line, output);
                break;
            case "undo":
                requireArguments(line.split("\\s+"), 1, "undo");
                output.println("Undone: " + crobs.undo());
                break;
            case "redo":
                requireArguments(line.split("\\s+"), 1, "redo");
                output.println("Redone: " + crobs.redo());
                break;
            default:
                executeRoomCommand(command, line, output);
        }
        return true;
    }

    private void executeUserCommand(String command, String line, PrintWriter output) {
        String[] args = line.split("\\s+");
        switch (command) {
            case "addUser":
                args = line.split("\\s+", 2);
                requireArguments(args, 2, "addUser name");
                output.println("User created: " + crobs.addUser(args[1]).getUserId());
                break;
            case "removeUser":
                requireArguments(args, 2, "removeUser targetUserID");
                crobs.removeUser(args[1]);
                output.println("User removed: " + args[1]);
                break;
            case "changeRole":
                requireArguments(args, 3, "changeRole targetUserId newRole");
                User changed = crobs.changeRole(args[1], args[2]);
                output.println("Role changed: " + changed.getUserId() + " (" + changed.getRole() + ")");
                break;
            case "login":
                requireArguments(args, 2, "login userID");
                User loggedIn = crobs.login(args[1]);
                output.println("Logged in: " + loggedIn.getUserId() + " (" + loggedIn.getRole() + ")");
                break;
            default:
                requireArguments(args, 1, "logout");
                crobs.logout();
                output.println("Logged out.");
        }
    }

    private void executeBookingCommand(String command, String line, PrintWriter output) {
        String[] args;
        switch (command) {
            case "book":
                args = line.split("\\s+", 6);
                requireArguments(args, 6, "book roomId date startTime endTime purpose");
                Booking created = crobs.book(args[1], LocalDate.parse(args[2]),
                        LocalTime.parse(args[3]), LocalTime.parse(args[4]), args[5]);
                output.println("Booking created: " + created.getBookingId());
                break;
            case "modifyBooking":
                args = line.split("\\s+", 7);
                requireArguments(args, 7, "modifyBooking bookingID roomId date startTime endTime purpose");
                Booking modified = crobs.modifyBooking(args[1], args[2], LocalDate.parse(args[3]),
                        LocalTime.parse(args[4]), LocalTime.parse(args[5]), args[6]);
                output.println("Booking modified: " + modified.getBookingId());
                break;
            case "deleteBooking":
                args = line.split("\\s+");
                requireArguments(args, 2, "deleteBooking bookingID");
                crobs.deleteBooking(args[1]);
                output.println("Booking deleted: " + args[1]);
                break;
            case "listBookings":
                requireArguments(line.split("\\s+"), 1, "listBookings");
                printBookings(crobs.listBookings(), output);
                break;
            default:
                args = line.split("\\s+");
                requireArguments(args, 3, "schedule roomId weekStartDate");
                printSchedule(crobs.schedule(args[1], LocalDate.parse(args[2])), output);
        }
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

    private static void printBookings(List<Booking> result, PrintWriter output) {
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

    private static void printSchedule(List<ScheduleEntry> result, PrintWriter output) {
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
