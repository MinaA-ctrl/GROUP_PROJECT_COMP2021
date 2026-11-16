package hk.edu.polyu.comp.comp2021.crobs.logging;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void acceptsOptionalGuiFlagAnywhere() {
        assertFalse(LogFileOptions.parse(new String[]{"-html", "a.html", "-txt", "b.txt"}).isGuiMode());
        assertTrue(LogFileOptions.parse(new String[]{"-gui", "-html", "a.html", "-txt", "b.txt"}).isGuiMode());
        LogFileOptions last = LogFileOptions.parse(new String[]{"-html", "a.html", "-txt", "b.txt", "-gui"});
        assertTrue(last.isGuiMode());
        assertEquals(Paths.get("b.txt"), last.getTextPath());
        assertThrows(IllegalArgumentException.class, () -> LogFileOptions.parse(new String[]{
                "-gui", "-html", "a", "-txt", "b", "-gui"}));
        assertThrows(IllegalArgumentException.class, () -> LogFileOptions.parse(new String[]{
                "-html", "-gui", "-txt", "b"}));
        assertThrows(IllegalArgumentException.class, () -> LogFileOptions.parse(new String[]{"-gui"}));
    }

    @Test
    void acceptsPortalFlagButNotBothGuis() {
        LogFileOptions portal = LogFileOptions.parse(new String[]{"-html", "a.html", "-txt", "b.txt", "-gui2"});
        assertTrue(portal.isPortalMode());
        assertFalse(portal.isGuiMode());
        assertFalse(LogFileOptions.parse(new String[]{"-gui", "-html", "a", "-txt", "b"}).isPortalMode());
        assertTrue(LogFileOptions.parse(new String[]{"-gui2", "-txt", "b", "-html", "a"}).isPortalMode());
        assertThrows(IllegalArgumentException.class, () -> LogFileOptions.parse(new String[]{
                "-gui", "-gui2", "-html", "a", "-txt", "b"}));
        assertThrows(IllegalArgumentException.class, () -> LogFileOptions.parse(new String[]{
                "-gui2", "-html", "a", "-txt", "b", "-gui2"}));
        assertThrows(IllegalArgumentException.class, () -> LogFileOptions.parse(new String[]{
                "-html", "-gui2", "-txt", "b"}));
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
