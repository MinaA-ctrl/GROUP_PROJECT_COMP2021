# User commands

The default application creates one superadmin with ID `sd00000000` and starts
logged out. The prompt prints that ID at startup. User records and the active
login live in memory for the current session.

| Command | Behavior |
| --- | --- |
| `login userID` | Activates an existing account. A failed login leaves the current session unchanged. |
| `logout` | Clears the active login. |
| `addUser name` | An admin or superadmin creates a regular user and receives their generated ID. Names can contain spaces. |
| `removeUser targetUserID` | An admin or superadmin removes a regular user and cancels all bookings owned by that user. |
| `changeRole targetUserId newRole` | The superadmin changes another user to `regular` or `admin`. Role names are case-insensitive. |

Regular accounts receive IDs such as `rg00000000`. Changing a role leaves the ID
unchanged, so bookings still point to the same user. Removing a user never reuses
their ID within the session. The superadmin cannot be removed or reassigned, and
no other account can become superadmin.

`UserManager` validates the command before changing account state. On removal,
it calls the booking cleanup supplied by `CROBS` before deleting the account.
Room permissions, booking ownership and schedule visibility use the active
session. The command loop logs all entered commands through the existing HTML
and TXT logger.
