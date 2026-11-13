package hk.edu.polyu.comp.comp2021.crobs.model;

/** A user record with an ID that is independent of its current role. */
public final class User {

    private final String userId;
    private final String name;
    private final UserType role;

    User(String userId, String name, UserType role) {
        this.userId = userId;
        this.name = name;
        this.role = role;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public UserType getRole() {
        return role;
    }
}
