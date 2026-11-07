# Room commands and search

`CROBS` coordinates room operations and booking availability. Its injected
current-user and admin callbacks must read the live user session. The default
constructor still has no session until the user manager is connected.

The command controller now takes a `CROBS` instance rather than a `BookingManager`.
All room commands require login. Only admins and superadmins may add, modify,
or delete rooms. Listing and searching are available to all logged-in users.

| Command | CROBS method |
| --- | --- |
| `addRoom roomType capacity features` | `addRoom(roomType, capacity, features)` |
| `modifyRoom roomID field newValue` | `modifyRoom(roomID, field, newValue)` |
| `deleteRoom roomID` | `deleteRoom(roomID)` |
| `listRoom roomID` | `listRoom(roomID)` |
| `listAllRooms` | `listAllRooms()` |
| `search date startTime endTime minCapacity features` | `search(date, start, end, minCapacity, features)` |

Room types use the existing `RoomType.fromString` mapping. Editable fields are
`roomType`, `capacity`, and `features`; modification preserves the room ID.
Capacity must be positive. Minimum search capacity can be zero but not negative.

Features use commas without spaces. Names are case-sensitive and duplicates
are removed. As a local input convention, `-` means no features in `addRoom`,
`modifyRoom ... features -`, or `search`; the brief does not prescribe an empty
feature-list token. The room model also accepts an empty feature string.

`RoomList.containsRoom(String)` provides the booking manager's existence check.
`getFeatures()` and `getAllRooms()` return list copies. Room-list results are
sorted by room ID. Room mutation should go through `CROBS` for permission checks;
the underlying `RoomList` is a model component, not an unrestricted CLI endpoint.

Search requires all requested features, sufficient capacity, and no overlapping
booking. It validates the time interval even when there are no candidate rooms.

Room deletion validates admin access and room existence, captures the current
time through `Clock`, cancels future bookings, and removes the room. As in the
booking support, a booking starting exactly at the cutoff is cancelled; an
already-started booking is retained. The extra `CROBS` constructor accepting a
`Clock` allows deterministic tests of this boundary.

Coordinate these edits with the room author before both branches change
`Room`, `RoomList`, or `CROBS`. Their existing ID format and getter names are kept.

Run `RoomCommandsTest` and `RoomCommandInterfaceTest` with JUnit 5. They cover
room permissions, stable IDs after edits, safe list access, sorted results,
capacity/feature/availability filtering, deletion cleanup, and command logging.
