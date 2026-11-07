# Operation logging (REQ1)

`logging.OperationLogger` writes a session's commands to two UTF-8 files:

- HTML: a table with operation index and operation command, numbered from 1.
- TXT: one original command per line, in execution order.

Each session replaces existing file contents. Use new filenames to keep older
logs. The destination directories must exist, and the paths must identify
different files. HTML command text is escaped; TXT preserves the original text.
Each call flushes both logs. `close()` completes the HTML document.

## Connecting the command interface

`LogFileOptions.parse(args)` accepts `-html path -txt path` in either order.
For filenames containing spaces, quote the filename in the launch arguments:

```text
-html "my logs/session.html" -txt "my logs/session.txt"
```

The command controller should create one logger for the whole session and call
`log(rawLine)` exactly once before dispatching every command, including invalid
commands, rejected operations, and `quit`. Do not log the launch arguments or
program output. Do not log a command separately in individual managers.

`Application` now performs this wiring using `CommandLineInterface`:

```java
LogFileOptions options = LogFileOptions.parse(args);
try (OperationLogger logger = new OperationLogger(
        options.getHtmlPath(), options.getTextPath())) {
    new CommandLineInterface(crobs.getBookingManager()).run(input, output, logger);
}
```

The controller should report malformed command errors and continue the session.
Invalid launch options or an inability to create/write logs should be reported
clearly and stop startup/the session. Files cannot be updated atomically as a
pair: an I/O failure may leave one log with a partially recorded final operation.
After a write failure, the logger rejects more commands; it can still be closed.
Try-with-resources closes both logs on quit, end of input, and exceptions.

`Application` accepts the launch flags and runs the command loop. The current
controller supports `book`, `modifyBooking`, `deleteBooking`, `listBookings`,
`schedule`, and `quit`. Dates use YYYY-MM-DD and times use HH:mm. A booking's
purpose can contain spaces. Rejected commands are logged and the loop continues.

The default `CROBS` still starts without a user/session implementation, so
booking commands report that login is required. Login, logout, user-management,
room-management, and full search commands must still be connected. Do not treat
this as a complete implementation of all project requirements. The injectable
`CROBS(RoomList, Supplier<String>, BooleanSupplier)` constructor connects a real
room collection and live session when the user manager is available.

To try logging now, run `Application` in IntelliJ with Program arguments
`-html session.html -txt session.txt`. Enter `listBookings`, an unknown command,
and `quit`; then inspect both generated files. Relative paths use the run
configuration's working directory. Avoid committing generated session logs.

Run `OperationLoggerTest`, `LogFileOptionsTest`, `CommandLineInterfaceTest`, and
`ApplicationTest` with JUnit 5 in IntelliJ. The command tests supply a test session
and exercise real room/booking objects; no test login bypass is shipped in the app.
