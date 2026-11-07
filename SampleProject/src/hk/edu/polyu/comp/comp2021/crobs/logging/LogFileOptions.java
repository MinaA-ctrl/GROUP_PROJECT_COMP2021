package hk.edu.polyu.comp.comp2021.crobs.logging;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/** Parses the required -html and -txt application arguments in either order. */
public final class LogFileOptions {

    private final Path htmlPath;
    private final Path textPath;

    private LogFileOptions(Path htmlPath, Path textPath) {
        this.htmlPath = htmlPath;
        this.textPath = textPath;
    }

    public static LogFileOptions parse(String[] args) {
        Objects.requireNonNull(args, "args");
        if (args.length != 4) {
            throw new IllegalArgumentException("Usage: -html <HTML file> -txt <TXT file>");
        }
        Path html = null;
        Path text = null;
        for (int i = 0; i < args.length; i += 2) {
            String flag = args[i];
            String value = Objects.requireNonNull(args[i + 1], "log path");
            if (value.trim().isEmpty() || "-html".equals(value) || "-txt".equals(value)) {
                throw new IllegalArgumentException("Missing file path for " + flag);
            }
            if ("-html".equals(flag) && html == null) {
                html = Paths.get(value);
            } else if ("-txt".equals(flag) && text == null) {
                text = Paths.get(value);
            } else {
                throw new IllegalArgumentException("Unknown or repeated option: " + flag);
            }
        }
        return new LogFileOptions(html, text);
    }

    public Path getHtmlPath() {
        return htmlPath;
    }

    public Path getTextPath() {
        return textPath;
    }
}
