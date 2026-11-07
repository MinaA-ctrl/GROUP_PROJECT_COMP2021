# Connecting booking operations

The booking model supports REQ10-12 and REQ17, availability checks for REQ13,
the weekly schedule in REQ14, and cancellation hooks for REQ3 and REQ9.
It still needs the real session, rooms, and command controller to be connected.

## Create one manager per application session

Use the three-argument `BookingManager` constructor with:

1. A `Supplier<String>` that returns the currently logged-in user's ID, or null
   when nobody is logged in.
2. A `Predicate<String>` that checks whether a room ID currently exists.
3. A `BooleanSupplier` that checks whether the current user is an admin or
   superadmin. Read this from the session's role, never from command input.

For example, if the eventual class APIs provide these methods:

```java
BookingManager bookings = new BookingManager(
        session::getCurrentUserId,
        rooms::containsRoom,
        session::isCurrentUserAdmin);
```

These session and room method names are proposals, not existing implementations.
All callbacks must read current state rather than capturing a fixed login or role.
The existing two-argument constructor remains usable, but grants no admin access.

## User commands

| Command | Model method | Notes |
| --- | --- | --- |
| `book` | `book(roomId, date, start, end, purpose)` | ID generated automatically; owner comes from session. |
| `modifyBooking` | `modifyBooking(bookingId, roomId, date, start, end, purpose)` | Owner only; failed changes preserve the original. |
| `deleteBooking` | `deleteBooking(bookingId)` | Owner only, including for admin users. |
| `listBookings` | `listBookings()` | Current user's bookings ordered by date and start. |
| `schedule` | `schedule(roomId, weekStartDate)` | Seven days including the given start date; no Monday requirement. |
| `search` | `isRoomAvailable(roomId, date, start, end)` | Combine with the room manager's capacity and feature checks. |

All these operations require a logged-in user. Dates use `LocalDate`; times use
`LocalTime`. The command controller must parse and validate textual input and
display useful messages for errors instead of terminating the application.

Two intervals overlap only when `newStart < existingEnd && existingStart < newEnd`
for the same room and date. Adjacent intervals are allowed. No extra restriction
against past dates or permitted hours is imposed by this model.

## Schedule privacy

`schedule` returns immutable `ScheduleEntry` snapshots ordered by date and time.
For a regular viewer, `getOwnerUserId()` returns null for someone else's booking.
Display a placeholder such as `hidden` for that value. Admins and superadmins can
see all owner IDs. A regular viewer can still see their own owner ID.

## Removing users and rooms

Both cancellation hooks require a logged-in admin or superadmin.

- The user manager validates that the target is an existing removable regular
  user, then calls `cancelBookingsForUser(userId)` as part of the removal. This
  removes all that user's bookings across every date and room.
- The room manager validates the target room, captures `LocalDateTime.now()`
  once, and calls `cancelFutureBookingsForRoom(roomId, cutoff)` as part of deletion.
  This cancels bookings starting at or after the cutoff. Past and already-started
  bookings are retained. Agree on this boundary if the instructor clarifies it.
- Each hook returns the number removed and can safely be called again. These
  hooks intentionally do not look up a user or room that may already be removed.
- The controller must not expose these hooks as unrestricted user commands.

Deleted bookings disappear from availability and schedule results. Their IDs are
never reused during the manager's lifetime. Create only one manager for the session.

## Checking the implementation

Run `BookingManagerTest` and `BookingSupportTest` with JUnit 5 in IntelliJ.
The latter covers schedule boundaries and privacy, availability, deletion cutoff
behavior, and authorization of cancellation hooks. Final verification should use
the assignment's Java 25 SDK and language level 8.

CLI wiring, HTML/TXT command logs, and full application testing remain separate
integration tasks. The room and user placeholders have not been implemented here.
