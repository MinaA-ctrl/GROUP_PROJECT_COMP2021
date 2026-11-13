package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** The user manager is responsible for the account records and active login for one CROBS session. */
final class UserManager {

    static final String SUPERADMIN_ID = "sd00000000";

    private final Map<String, User> users = new HashMap<>();
    private long nextUserNumber;
    private String currentUserId;

    UserManager() {
        users.put(SUPERADMIN_ID, new User(SUPERADMIN_ID, "Superadmin", UserType.SUPERADMIN));
    }

    String getCurrentUserId() {
        return currentUserId;
    }

    boolean isCurrentUserAdmin() {
        User current = users.get(currentUserId);
        return current != null && current.getRole().isAdmin();
    }

    User login(String userId) {
        User user = requireUser(userId);
        currentUserId = user.getUserId();
        return user;
    }

    void logout() {
        if (currentUserId == null) {
            throw new IllegalStateException("No user is logged in");
        }
        currentUserId = null;
    }

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

    void removeUser(String userId, Consumer<String> cancelBookings) {
        requireAdmin();
        User target = requireUser(userId);
        if (target.getRole() != UserType.REGULAR) {
            throw new IllegalArgumentException("Only regular users can be removed");
        }
        cancelBookings.accept(userId);
        users.remove(userId);
    }

    User changeRole(String userId, String newRole) {
        if (!SUPERADMIN_ID.equals(currentUserId)) {
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
