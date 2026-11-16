package hk.edu.polyu.comp.comp2021.crobs.logging;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * The annoying process that parses the required -html and -txt application arguments in
 * either order, plus the optional -gui flag (classic GUI) or -gui2 flag (UNI portal GUI).
 */
public final class LogFileOptions {

    private static final String USAGE = "Usage: -html <HTML file> -txt <TXT file> [-gui | -gui2]";

    private final Path htmlPath;
    private final Path textPath;
    private final boolean guiMode;
    private final boolean portalMode;

    private LogFileOptions(Path htmlPath, Path textPath, boolean guiMode, boolean portalMode) {
        this.htmlPath = htmlPath;
        this.textPath = textPath;
        this.guiMode = guiMode;
        this.portalMode = portalMode;
    }

    /**
     * Parses the launch arguments.
     *
     * @param args the arguments given to the application
     * @return the parsed options
     * @throws IllegalArgumentException if an option is missing, unknown, repeated or has no value,
     *         or if both -gui and -gui2 are given
     */
    public static LogFileOptions parse(String[] args) {
        Objects.requireNonNull(args, "args");
        Path html = null;
        Path text = null;
        boolean gui = false;
        boolean portal = false;
        int index = 0;
        while (index < args.length) {
            String flag = args[index];
            if ("-gui".equals(flag)) {
                if (gui) {
                    throw new IllegalArgumentException("Unknown or repeated option: " + flag);
                }
                gui = true;
                index++;
                continue;
            }
            if ("-gui2".equals(flag)) {
                if (portal) {
                    throw new IllegalArgumentException("Unknown or repeated option: " + flag);
                }
                portal = true;
                index++;
                continue;
            }
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException(USAGE);
            }
            String value = Objects.requireNonNull(args[index + 1], "log path");
            if (value.trim().isEmpty() || isOption(value)) {
                throw new IllegalArgumentException("Missing file path for " + flag);
            }
            if ("-html".equals(flag) && html == null) {
                html = Paths.get(value);
            } else if ("-txt".equals(flag) && text == null) {
                text = Paths.get(value);
            } else {
                throw new IllegalArgumentException("Unknown or repeated option: " + flag);
            }
            index += 2;
        }
        if (html == null || text == null) {
            throw new IllegalArgumentException(USAGE);
        }
        if (gui && portal) {
            throw new IllegalArgumentException("Use either -gui or -gui2, not both");
        }
        return new LogFileOptions(html, text, gui, portal);
    }

    private static boolean isOption(String value) {
        return "-html".equals(value) || "-txt".equals(value) || "-gui".equals(value)
                || "-gui2".equals(value);
    }

    /**
     * Returns the path of the HTML log.
     *
     * @return the HTML log file
     */
    public Path getHtmlPath() {
        return htmlPath;
    }

    /**
     * Returns the path of the plain-text log.
     *
     * @return the TXT log file
     */
    public Path getTextPath() {
        return textPath;
    }

    /**
     * Tells whether the graphical user interface was requested.
     *
     * @return true if -gui was given
     */
    public boolean isGuiMode() {
        return guiMode;
    }

    /**
     * Tells whether the UNI portal GUI was requested.
     *
     * @return true if -gui2 was given
     */
    public boolean isPortalMode() {
        return portalMode;
    }
}
