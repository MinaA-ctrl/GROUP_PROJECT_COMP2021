package hk.edu.polyu.comp.comp2021.crobs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApplicationTest {

    @Test
    void startsWithArgumentsLogsErrorsAndQuitThenClosesHtml() throws IOException {
        Path directory = Files.createTempDirectory("crobs-app-");
        Path html = directory.resolve("session log.html");
        Path text = directory.resolve("session log.txt");
        StringWriter output = new StringWriter();
        StringWriter errors = new StringWriter();
        int status = Application.run(new String[]{"-txt", text.toString(), "-html", html.toString()},
                new StringReader("listBookings\nquit\nnot reached\n"),
                new PrintWriter(output), new PrintWriter(errors));
        assertEquals(0, status);
        assertEquals("", errors.toString());
        assertTrue(output.toString().contains("Log in before managing bookings"));
        assertEquals(Arrays.asList("listBookings", "quit"), Files.readAllLines(text, StandardCharsets.UTF_8));
        assertTrue(new String(Files.readAllBytes(html), StandardCharsets.UTF_8).endsWith("</html>\n"));
    }

    @Test
    void userCommandsWorkInTheDefaultApplicationAndAreLogged() throws IOException {
        Path directory = Files.createTempDirectory("crobs-app-users-");
        Path html = directory.resolve("session.html");
        Path text = directory.resolve("session.txt");
        String[] commands = {"login sd00000000", "addUser Alex Chan",
                "changeRole rg00000000 admin", "logout", "login rg00000000",
                "addRoom meetingRoom 20 projector", "listAllRooms", "login sd00000000",
                "changeRole rg00000000 regular", "removeUser rg00000000",
                "login rg00000000", "quit"};
        StringWriter output = new StringWriter();
        StringWriter errors = new StringWriter();
        assertEquals(0, Application.run(new String[]{"-html", html.toString(), "-txt", text.toString()},
                new StringReader(String.join("\n", commands)), new PrintWriter(output), new PrintWriter(errors)));
        assertEquals("", errors.toString());
        assertTrue(output.toString().contains("Superadmin user ID: sd00000000"));
        assertTrue(output.toString().contains("Logged in: rg00000000 (admin)"));
        assertTrue(output.toString().contains("meetingRoom\t20\tprojector"));
        assertTrue(output.toString().contains("User removed: rg00000000"));
        assertTrue(output.toString().contains("Error: Unknown user ID: rg00000000"));
        assertEquals(Arrays.asList(commands), Files.readAllLines(text, StandardCharsets.UTF_8));
        String htmlLog = new String(Files.readAllBytes(html), StandardCharsets.UTF_8);
        assertTrue(htmlLog.contains("<td>12</td><td>quit</td>"));
    }

    @Test
    void endOfInputClosesLogsWithoutQuit() throws IOException {
        Path directory = Files.createTempDirectory("crobs-app-eof-");
        Path html = directory.resolve("session.html");
        Path text = directory.resolve("session.txt");
        assertEquals(0, Application.run(new String[]{"-html", html.toString(), "-txt", text.toString()},
                new StringReader(""), new PrintWriter(new StringWriter()), new PrintWriter(new StringWriter())));
        assertEquals(0, Files.size(text));
        assertTrue(new String(Files.readAllBytes(html), StandardCharsets.UTF_8).contains("</html>"));
    }

    @Test
    void invalidLaunchOptionsReportErrorWithoutReadingInput() {
        StringWriter errors = new StringWriter();
        assertEquals(1, Application.run(new String[]{}, new StringReader("quit\n"),
                new PrintWriter(new StringWriter()), new PrintWriter(errors)));
        assertTrue(errors.toString().contains("Usage: -html"));
    }

    @Test
    void invalidLogDestinationReportsStartupFailure() throws IOException {
        Path directory = Files.createTempDirectory("crobs-app-path-");
        StringWriter errors = new StringWriter();
        assertEquals(1, Application.run(new String[]{"-html", directory.toString(),
                "-txt", directory.resolve("log.txt").toString()}, new StringReader("quit\n"),
                new PrintWriter(new StringWriter()), new PrintWriter(errors)));
        assertTrue(errors.toString().contains("input/output error"));
    }
}
