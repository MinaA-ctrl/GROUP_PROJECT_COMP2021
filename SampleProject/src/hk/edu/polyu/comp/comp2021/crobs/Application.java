package hk.edu.polyu.comp.comp2021.crobs;

import hk.edu.polyu.comp.comp2021.crobs.model.CROBS;
import hk.edu.polyu.comp.comp2021.crobs.controller.CommandLineInterface;
import hk.edu.polyu.comp.comp2021.crobs.logging.LogFileOptions;
import hk.edu.polyu.comp.comp2021.crobs.logging.OperationLogger;
import hk.edu.polyu.comp.comp2021.crobs.view.CrobsWindow;
import hk.edu.polyu.comp.comp2021.crobs.view.portal.PortalWindow;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * The entry point of Crobs. It starts the command-line interface, or with -gui the classic GUI,
 * or with -gui2 the UNI portal GUI.
 */
public final class Application {

    private Application() {
    }

    /**
     * Starts Crobs, e.g. with -html log.html -txt log.txt, optionally followed by -gui or -gui2.
     *
     * @param args the launch arguments
     */
    public static void main(String[] args) {
        int status = run(args, new InputStreamReader(System.in, StandardCharsets.UTF_8),
                new PrintWriter(System.out, true), new PrintWriter(System.err, true));
        if (status != 0) {
            System.exit(status);
        }
    }

    /**
     * The big run of a CROBS session without closing the supplied console streams. In GUI
     * mode (-gui or -gui2) the window is opened and this method returns at once; the window then owns
     * the logger.
     *
     * @param args the launch arguments
     * @param input where CLI commands are read from
     * @param output where CLI results are printed
     * @param errors where start-up errors are printed
     * @return 0 on success, 1 if Crobs could not start or stopped because of an input/output error
     */
    public static int run(String[] args, Reader input, PrintWriter output, PrintWriter errors) {
        try {
            LogFileOptions options = LogFileOptions.parse(args);
            CROBS crobs = new CROBS();
            if (options.isGuiMode() || options.isPortalMode()) {
                if (GraphicsEnvironment.isHeadless()) {
                    throw new IllegalArgumentException("GUI mode needs a graphical display");
                }
                OperationLogger logger = new OperationLogger(options.getHtmlPath(), options.getTextPath());
                if (options.isPortalMode()) {
                    PortalWindow.open(crobs, logger);
                } else {
                    CrobsWindow.open(crobs, logger);
                }
                return 0;
            }
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
