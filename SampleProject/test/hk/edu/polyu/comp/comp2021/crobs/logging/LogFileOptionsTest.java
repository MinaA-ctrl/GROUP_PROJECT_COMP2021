package hk.edu.polyu.comp.comp2021.crobs.logging;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LogFileOptionsTest {

    @Test
    void acceptsBothFlagOrdersAndPathsWithSpaces() {
        LogFileOptions normal = LogFileOptions.parse(new String[]{
                "-html", "my logs/session.html", "-txt", "my logs/session.txt"});
        LogFileOptions reversed = LogFileOptions.parse(new String[]{
                "-txt", "my logs/session.txt", "-html", "my logs/session.html"});
        assertEquals(Paths.get("my logs/session.html"), normal.getHtmlPath());
        assertEquals(Paths.get("my logs/session.txt"), normal.getTextPath());
        assertEquals(normal.getHtmlPath(), reversed.getHtmlPath());
        assertEquals(normal.getTextPath(), reversed.getTextPath());
    }

    @Test
    void rejectsMissingUnknownAndRepeatedFlags() {
        assertThrows(IllegalArgumentException.class, () -> LogFileOptions.parse(new String[]{}));
        assertThrows(IllegalArgumentException.class, () -> LogFileOptions.parse(new String[]{"-html", "a"}));
        assertThrows(IllegalArgumentException.class, () -> LogFileOptions.parse(new String[]{
                "-html", "a", "-html", "b"}));
        assertThrows(IllegalArgumentException.class, () -> LogFileOptions.parse(new String[]{
                "-html", "a", "-unknown", "b"}));
        assertThrows(IllegalArgumentException.class, () -> LogFileOptions.parse(new String[]{
                "-html", " ", "-txt", "b"}));
        assertThrows(IllegalArgumentException.class, () -> LogFileOptions.parse(new String[]{
                "-html", "-txt", "-txt", "b"}));
    }
}
