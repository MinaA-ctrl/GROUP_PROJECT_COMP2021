package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/** The user manager is responsible for the account records and active login for one CROBS session. */
final class UserManager {

    private static final String SUPERADMIN_ID = "sd00000000";

    private final Map<String, User> users = new HashMap<>();
    private long nextUserNumber;
    private Optional<String> currentUserId = Optional.empty();

    /** Creates the account store with the superadmin account and no active login. */
    UserManager() {
        users.put(SUPERADMIN_ID, new User(SUPERADMIN_ID, "Superadmin", UserType.SUPERADMIN));
    }

    /**
     * Returns the fixed ID of the superadmin account that exists from the start of every session.
     *
     * @return the superadmin user ID
     */
    static String superadminId() {
        return SUPERADMIN_ID;
    }

    /**
     * Returns the ID of the logged-in user.
     *
     * @return the current user ID, or null when nobody is logged in
     */
    String getCurrentUserId() {
        return currentUserId.orElse(null);
    }

    /**
     * Returns the logged-in user.
     *
     * @return the current user, or an empty optional when nobody is logged in
     */
    Optional<User> getCurrentUser() {
        return currentUserId.map(users::get);
    }

    /**
     * Checks whether the logged-in user has admin privileges.
     *
     * @return true for a logged-in admin or superadmin
     */
    boolean isCurrentUserAdmin() {
        return getCurrentUser().map(user -> user.getRole().isAdmin()).orElse(false);
    }

    /**
     * Logs in as an existing user.
     *
     * @param userId the ID of the user to log in as
     * @return the user who is now logged in
     * @throws IllegalArgumentException if the ID is unknown
     */
    User login(String userId) {
        User user = requireUser(userId);
        currentUserId = Optional.of(user.getUserId());
        return user;
    }

    /**
     * Logs out the current user.
     *
     * @throws IllegalStateException if nobody is logged in
     */
    void logout() {
        if (!currentUserId.isPresent()) {
            throw new IllegalStateException("No user is logged in");
        }
        currentUserId = Optional.empty();
    }

    /**
     * Creates a regular user with a generated ID (REQ2).
     *
     * @param name the display name of the new user
     * @return the new user
     * @throws IllegalStateException if the current user is not an admin
     * @throws IllegalArgumentException if the name is blank
     */
    User addUser(String name) {
        requireAdmin();
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("User name is required");
        }
        String id = String.format(Locale.ROOT, "rg%08x", nextUserNumber++);
        User user = new User(id, name.trim(), UserType.REGULAR);
        users.put(id, user);
        return user;
    }

    /**
     * Removes a regular user after cancelling their bookings (REQ3).
     *
     * @param userId the ID of the regular user to remove
     * @param cancelBookings called with the user ID to cancel that user's bookings
     * @throws IllegalStateException if the current user is not an admin
     * @throws IllegalArgumentException if the user is unknown or not a regular user
     */
    void removeUser(String userId, Consumer<String> cancelBookings) {
        requireAdmin();
        User target = requireUser(userId);
        if (target.getRole() != UserType.REGULAR) {
            throw new IllegalArgumentException("Only regular users can be removed");
        }
        cancelBookings.accept(userId);
        users.remove(userId);
    }

    /**
     * Changes the role of a user between regular and admin (REQ4).
     *
     * @param userId the ID of the user whose role changes
     * @param newRole the new role, regular or admin
     * @return the user with the new role
     * @throws IllegalStateException if the current user is not the superadmin
     * @throws IllegalArgumentException if the user or role is invalid
     */
    User changeRole(String userId, String newRole) {
        if (!currentUserId.filter(SUPERADMIN_ID::equals).isPresent()) {
            throw new IllegalStateException("Superadmin privileges are required");
        }
        User target = requireUser(userId);
        UserType role = UserType.fromString(newRole);
        if (target.getRole() == UserType.SUPERADMIN || role == UserType.SUPERADMIN) {
            throw new IllegalArgumentException("Change roles between regular and admin only");
        }
        User updated = new User(target.getUserId(), target.getName(), role);
        users.put(userId, updated);
        return updated;
    }

    /**
     * Copies all accounts for an undo snapshot. Users are immutable, so a shallow copy is enough.
     *
     * @return a new map from user ID to user
     */
    Map<String, User> copyAccounts() {
        return new HashMap<>(users);
    }

    /**
     * Replaces all accounts with saved ones. The active login and the ID counter are kept,
     * so IDs are never reused after an undo.
     *
     * @param savedAccounts the accounts to restore
     */
    void restoreAccounts(Map<String, User> savedAccounts) {
        users.clear();
        users.putAll(savedAccounts);
    }

    private User requireUser(String userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("Unknown user ID: " + userId);
        }
        return user;
    }

    private void requireAdmin() {
        if (!isCurrentUserAdmin()) {
            throw new IllegalStateException("Admin privileges are required");
        }
    }
}
