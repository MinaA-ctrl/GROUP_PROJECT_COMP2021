package hk.edu.polyu.comp.comp2021.crobs;

import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.controller.CommandLineInterface;
import hk.edu.polyu.comp.comp2021.crobs.logging.LogFileOptions;
import hk.edu.polyu.comp.comp2021.crobs.logging.OperationLogger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public final class Application {

    private Application() {
    }

    public static void main(String[] args) {
        int status = run(args, new InputStreamReader(System.in, StandardCharsets.UTF_8),
                new PrintWriter(System.out, true), new PrintWriter(System.err, true));
        if (status != 0) {
            System.exit(status);
        }
    }

    /** The big run of a CROBS session without closing the supplied console streams. */
    public static int run(String[] args, Reader input, PrintWriter output, PrintWriter errors) {
        try {
            LogFileOptions options = LogFileOptions.parse(args);
            CROBS crobs = new CROBS();
            try (OperationLogger logger = new OperationLogger(options.getHtmlPath(), options.getTextPath())) {
                new CommandLineInterface(crobs).run(input, output, logger);
            }
            return 0;
        } catch (IllegalArgumentException invalidOptions) {
            errors.println("Cannot start Crobs: " + invalidOptions.getMessage());
            errors.flush();
            return 1;
        } catch (IOException ioFailure) {
            errors.println("Crobs stopped because of an input/output error: " + ioFailure.getMessage());
            errors.flush();
            return 1;
        }
    }
}
