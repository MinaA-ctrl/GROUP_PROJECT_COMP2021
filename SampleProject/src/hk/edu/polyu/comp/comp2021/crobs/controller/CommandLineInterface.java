package hk.edu.polyu.comp.comp2021.crobs.controller;

import hk.edu.polyu.comp.comp2021.crobs.logging.OperationLogger;
import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Objects;

/** The CLI, of course, reads and logs commands and passes them to the command processor. */
public final class CommandLineInterface {

    private final CROBS crobs;
    private final CommandProcessor processor;

    /**
     * Creates a command-line interface for one CROBS session.
     *
     * @param crobs the model to operate on
     */
    public CommandLineInterface(CROBS crobs) {
        this.crobs = Objects.requireNonNull(crobs, "crobs");
        this.processor = new CommandProcessor(crobs);
    }

    /**
     * The CLI runs until quit or end of input. Input and output remain caller-owned;
     * the application's try-with-resources closes the logger. Logging failures
     * stop the loop before the affected command is executed.
     *
     * @param input where commands are read from
     * @param output where results are printed
     * @param logger records every command line
     * @throws IOException if reading a command or writing the logs fails
     */
    public void run(Reader input, PrintWriter output, OperationLogger logger) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(logger, "logger");
        BufferedReader reader = input instanceof BufferedReader
                ? (BufferedReader) input : new BufferedReader(input);
        output.println("Crobs. Type quit to exit.");
        output.println("Superadmin user ID: " + crobs.getSuperadminId());
        while (true) {
            output.print("crobs> ");
            output.flush();
            String rawLine = reader.readLine();
            if (rawLine == null) {
                return;
            }
            logger.log(rawLine);
            if (!processor.execute(rawLine, output)) {
                return;
            }
        }
    }
}
