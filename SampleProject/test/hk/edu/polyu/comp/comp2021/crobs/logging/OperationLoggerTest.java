package hk.edu.polyu.comp.comp2021.crobs.logging;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationLoggerTest {

    @Test
    void recordsCommandsInOrderAndFlushesBeforeClose() throws IOException {
        Path directory = Files.createTempDirectory("crobs-log-order-");
        Path html = directory.resolve("session.html");
        Path text = directory.resolve("session.txt");
        try (OperationLogger logger = new OperationLogger(html, text)) {
            logger.log("  login U1  ");
            logger.log("unknownCommand");
            logger.log("quit");
            assertEquals(Arrays.asList("  login U1  ", "unknownCommand", "quit"),
                    Files.readAllLines(text, StandardCharsets.UTF_8));
            String content = read(html);
            assertTrue(content.contains("<tr><td>1</td><td>  login U1  </td></tr>"));
            assertTrue(content.contains("<tr><td>2</td><td>unknownCommand</td></tr>"));
            assertTrue(content.contains("<tr><td>3</td><td>quit</td></tr>"));
        }
        assertTrue(read(html).endsWith("</tbody>\n</table>\n</body>\n</html>\n"));
    }

    @Test
    void escapesHtmlButPreservesRawTextAndUnicode() throws IOException {
        Path directory = Files.createTempDirectory("crobs-log-escape-");
        Path html = directory.resolve("session.html");
        Path text = directory.resolve("session.txt");
        String command = "book R1 <script> & \"quoted\" 'purpose' \u8ba8\u8bba";
        try (OperationLogger logger = new OperationLogger(html, text)) {
            logger.log(command);
        }
        assertEquals(Collections.singletonList(command), Files.readAllLines(text, StandardCharsets.UTF_8));
        String content = read(html);
        assertFalse(content.contains("<script>"));
        assertTrue(content.contains("&lt;script&gt; &amp; &quot;quoted&quot; &#39;purpose&#39; \u8ba8\u8bba"));
    }

    @Test
    void newSessionOverwritesOldLogsAndStartsAtOne() throws IOException {
        Path directory = Files.createTempDirectory("crobs-log-session-");
        Path html = directory.resolve("session.html");
        Path text = directory.resolve("session.txt");
        try (OperationLogger logger = new OperationLogger(html, text)) {
            logger.log("old command");
        }
        try (OperationLogger logger = new OperationLogger(html, text)) {
            logger.log("new command");
        }
        assertEquals(Collections.singletonList("new command"), Files.readAllLines(text, StandardCharsets.UTF_8));
        assertFalse(read(html).contains("old command"));
        assertTrue(read(html).contains("<tr><td>1</td><td>new command</td></tr>"));
    }

    @Test
    void handlesEmptySessionAndRepeatedCloseButRejectsLoggingAfterClose() throws IOException {
        Path directory = Files.createTempDirectory("crobs-log-close-");
        Path html = directory.resolve("session.html");
        Path text = directory.resolve("session.txt");
        OperationLogger logger = new OperationLogger(html, text);
        logger.close();
        logger.close();
        assertEquals(0, Files.size(text));
        assertTrue(read(html).contains("</html>"));
        assertThrows(IOException.class, () -> logger.log("quit"));
    }

    @Test
    void rejectsIdenticalPathsBeforeChangingExistingFile() throws IOException {
        Path directory = Files.createTempDirectory("crobs-log-path-");
        Path file = directory.resolve("same.log");
        Files.write(file, Collections.singletonList("keep me"), StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> new OperationLogger(file,
                directory.resolve(".").resolve("same.log")));
        assertEquals(Collections.singletonList("keep me"), Files.readAllLines(file, StandardCharsets.UTF_8));
    }

    @Test
    void rejectsMultilineCommandsWithoutConsumingAnIndex() throws IOException {
        Path directory = Files.createTempDirectory("crobs-log-lines-");
        Path html = directory.resolve("session.html");
        Path text = directory.resolve("session.txt");
        try (OperationLogger logger = new OperationLogger(html, text)) {
            assertThrows(IllegalArgumentException.class, () -> logger.log("login U1\nquit"));
            assertThrows(IllegalArgumentException.class, () -> logger.log("login U1\rquit"));
            assertThrows(NullPointerException.class, () -> logger.log(null));
            logger.log("quit");
        }
        assertEquals(Collections.singletonList("quit"), Files.readAllLines(text, StandardCharsets.UTF_8));
        assertTrue(read(html).contains("<tr><td>1</td><td>quit</td></tr>"));
    }

    @Test
    void reportsUnwritableDestination() throws IOException {
        Path directory = Files.createTempDirectory("crobs-log-error-");
        assertThrows(IOException.class, () -> new OperationLogger(directory.resolve("session.html"), directory));
        assertThrows(IOException.class, () -> new OperationLogger(directory,
                directory.resolve("session.txt")));
    }

    private static String read(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }
}
