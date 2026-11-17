package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Locale;

/** The access roles a user can have. */
public enum UserType {
    /** A regular user who can manage only their own bookings. */
    REGULAR,
    /** An admin user who can also manage rooms and regular users. */
    ADMIN,
    /** The unique superadmin, an admin who can also change roles. */
    SUPERADMIN;

    /**
     * Checks whether this role has admin privileges.
     *
     * @return true for admin and superadmin
     */
    boolean isAdmin() {
        return this == ADMIN || this == SUPERADMIN;
    }

    /**
     * Converts a role name, ignoring case.
     *
     * @param value the role name typed by the user
     * @return the matching role
     * @throws IllegalArgumentException if no role has that name
     */
    static UserType fromString(String value) {
        for (UserType role : values()) {
            if (role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown user role: " + value);
    }

    /**
     * Returns the role name in lower case, as shown to users.
     *
     * @return the lower-case role name
     */
    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
