package hk.edu.polyu.comp.comp2021.crobs.model;

/** A user record with an ID that is independent of its current role. */
public final class User {

    private final String userId;
    private final String name;
    private final UserType role;

    /**
     * Creates a user record. Only the user manager creates users.
     *
     * @param userId the unique user ID
     * @param name the display name
     * @param role the access role
     */
    User(String userId, String name, UserType role) {
        this.userId = userId;
        this.name = name;
        this.role = role;
    }

    /**
     * Returns the unique user ID.
     *
     * @return the user ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Returns the display name.
     *
     * @return the user's name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the access role.
     *
     * @return the user's role
     */
    public UserType getRole() {
        return role;
    }
}
