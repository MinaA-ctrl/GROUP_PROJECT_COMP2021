package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.Locale;

public enum UserType {
    REGULAR, ADMIN, SUPERADMIN;

    boolean isAdmin() {
        return this == ADMIN || this == SUPERADMIN;
    }

    static UserType fromString(String value) {
        for (UserType role : values()) {
            if (role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown user role: " + value);
    }

    @Override
    public String toString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
