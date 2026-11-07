package hk.edu.polyu.comp.comp2021.crobs.logging;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** Writes one session's commands to the two files required by REQ1. */
public final class OperationLogger implements Closeable {

    private final BufferedWriter htmlWriter;
    private final BufferedWriter textWriter;
    private long nextIndex = 1;
    private boolean closed;
    private boolean failed;

    /**
     * Creates UTF-8 logs, replacing any existing contents. Parent directories
     * must already exist. The two paths must identify different files.
     */
    public OperationLogger(Path htmlPath, Path textPath) throws IOException {
        Objects.requireNonNull(htmlPath, "htmlPath");
        Objects.requireNonNull(textPath, "textPath");
        if (htmlPath.toAbsolutePath().normalize().equals(textPath.toAbsolutePath().normalize())
                || (Files.exists(htmlPath) && Files.exists(textPath)
                && Files.isSameFile(htmlPath, textPath))) {
            throw new IllegalArgumentException("HTML and TXT logs must use different files");
        }

        BufferedWriter html = Files.newBufferedWriter(htmlPath, StandardCharsets.UTF_8);
        BufferedWriter text = null;
        try {
            text = Files.newBufferedWriter(textPath, StandardCharsets.UTF_8);
            html.write("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n"
                    + "<meta charset=\"UTF-8\">\n<title>Crobs operation log</title>\n"
                    + "</head>\n<body>\n<table>\n<thead><tr>"
                    + "<th>Operation index</th><th>Operation command</th>"
                    + "</tr></thead>\n<tbody>\n");
            html.flush();
        } catch (IOException failure) {
            try {
                html.close();
            } catch (IOException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            if (text != null) {
                try {
                    text.close();
                } catch (IOException closeFailure) {
                    failure.addSuppressed(closeFailure);
                }
            }
            throw failure;
        }
        htmlWriter = html;
        textWriter = text;
    }

    /**
     * Records one raw command line, including its original whitespace.
     * Call once before dispatch so invalid commands and quit are recorded too.
     * Each successful call flushes both files immediately.
     */
    public void log(String command) throws IOException {
        if (closed || failed) {
            throw new IOException("Operation logger is closed or has failed");
        }
        Objects.requireNonNull(command, "command");
        if (command.indexOf('\n') >= 0 || command.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Log one command line at a time");
        }
        try {
            htmlWriter.write("<tr><td>" + nextIndex + "</td><td>"
                    + escapeHtml(command) + "</td></tr>\n");
            textWriter.write(command);
            textWriter.newLine();
            htmlWriter.flush();
            textWriter.flush();
            nextIndex++;
        } catch (IOException failure) {
            failed = true;
            throw failure;
        }
    }

    /** Completes the HTML document and attempts to close both files. */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        IOException failure = null;
        try {
            htmlWriter.write("</tbody>\n</table>\n</body>\n</html>\n");
        } catch (IOException writeFailure) {
            failure = writeFailure;
        }
        try {
            htmlWriter.close();
        } catch (IOException closeFailure) {
            if (failure == null) {
                failure = closeFailure;
            } else {
                failure.addSuppressed(closeFailure);
            }
        }
        try {
            textWriter.close();
        } catch (IOException closeFailure) {
            if (failure == null) {
                failure = closeFailure;
            } else {
                failure.addSuppressed(closeFailure);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
