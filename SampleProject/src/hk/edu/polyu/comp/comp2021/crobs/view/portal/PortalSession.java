package hk.edu.polyu.comp.comp2021.crobs.view.portal;

import hk.edu.polyu.comp.comp2021.crobs.controller.CommandProcessor;
import hk.edu.polyu.comp.comp2021.crobs.logging.OperationLogger;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Runs the portal's actions as ordinary Crobs commands: each command is logged with the
 * {@link OperationLogger} and executed by the shared {@link CommandProcessor}, exactly as in
 * the command-line interface, so REQ1 logging and all error handling are identical.
 */
final class PortalSession {

    private static final String ERROR_PREFIX = "Error:";

    private final CROBS crobs;
    private final CommandProcessor processor;
    private final OperationLogger logger;
    private final List<Consumer<Result>> listeners = new ArrayList<>();
    private boolean closed;

    /**
     * Creates a session that owns the logger.
     *
     * @param crobs the model
     * @param logger the logger that records every command
     */
    PortalSession(CROBS crobs, OperationLogger logger) {
        this.crobs = Objects.requireNonNull(crobs, "crobs");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.processor = new CommandProcessor(crobs);
    }

    /**
     * Returns the model, which pages read to fill their tables.
     *
     * @return the model
     */
    CROBS model() {
        return crobs;
    }

    /**
     * Registers a listener that is told about every executed command.
     *
     * @param listener receives the result of each command
     */
    void addListener(Consumer<Result> listener) {
        listeners.add(listener);
    }

    /**
     * Tells whether the session has ended because of quit or a logging failure.
     *
     * @return true once the logger is closed
     */
    boolean isClosed() {
        return closed;
    }

    /**
     * Logs and executes one command, then notifies the listeners.
     *
     * @param command the command text, e.g. "book lt00000000 2026-12-01 09:00 10:00 Lecture"
     * @return the result of the command
     */
    Result run(String command) {
        if (closed) {
            return new Result(command, "Error: the session has ended", false);
        }
        Result result;
        try {
            logger.log(command);
            StringWriter buffer = new StringWriter();
            boolean keepRunning = processor.execute(command, new PrintWriter(buffer));
            String output = buffer.toString().trim();
            result = new Result(command, output, !output.startsWith(ERROR_PREFIX));
            if (!keepRunning) {
                close();
            }
        } catch (IOException | IllegalArgumentException logFailure) {
            result = new Result(command, "Error: cannot write the log files: " + logFailure.getMessage(), false);
            close();
        }
        for (Consumer<Result> listener : listeners) {
            listener.accept(result);
        }
        return result;
    }

    /** Completes and closes the log files; later commands are refused. */
    void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            logger.close();
        } catch (IOException ignoredBecauseClosing) {
            closed = true;
        }
    }

    /** The outcome of one command: its text, Crobs' answer and whether it succeeded. */
    static final class Result {

        private final String command;
        private final String output;
        private final boolean success;

        /**
         * Creates a result.
         *
         * @param command the command that was run
         * @param output the text Crobs printed
         * @param success false if Crobs printed an error
         */
        Result(String command, String output, boolean success) {
            this.command = command;
            this.output = output;
            this.success = success;
        }

        /**
         * Returns the command that was run.
         *
         * @return the command text
         */
        String getCommand() {
            return command;
        }

        /**
         * Returns the text Crobs printed.
         *
         * @return the output, without a trailing newline
         */
        String getOutput() {
            return output;
        }

        /**
         * Tells whether the command succeeded.
         *
         * @return false if Crobs printed an error
         */
        boolean isSuccess() {
            return success;
        }

        /**
         * Returns the message to show in the status bar: the first line of the output without
         * the "Error: " prefix, or the number of results for commands that print a table.
         *
         * @return a short message
         */
        String getMessage() {
            String[] lines = output.split("\\R");
            if (lines.length > 0 && lines[0].contains("\t")) {
                int rows = lines.length - 1;
                return command.trim().split("\\s+")[0] + ": " + rows + (rows == 1 ? " result" : " results");
            }
            String first = output.isEmpty() ? command : lines[0];
            return first.startsWith(ERROR_PREFIX) ? first.substring(ERROR_PREFIX.length()).trim() : first;
        }
    }
}
