package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.ArrayList;

/** A bookable room with a fixed ID and an editable type, capacity and feature list. */
public class Room {
    private int capacity;
    private RoomType roomType;
    private ArrayList<String> features = new ArrayList<String>();
    private final String roomID;

    /**
     * Creates a room without features.
     *
     * @param capacity the positive capacity
     * @param type the room type
     * @throws IllegalArgumentException if the capacity or type is invalid
     */
    Room(int capacity, RoomType type){
        this(capacity, type, "");
    }

    /**
     * Creates a room and generates its unique ID.
     *
     * @param capacity the positive capacity
     * @param type the room type
     * @param features comma-separated features without spaces, or - for none
     * @throws IllegalArgumentException if any argument is invalid
     */
    Room(int capacity, RoomType type, String features){
        this.capacity = checkCapacity(capacity);
        if (type == null) {
            throw new IllegalArgumentException("Room type is required");
        }
        this.roomType = type;
        this.features = parseFeatures(features);
        this.roomID = generateRoomID(type); // last, so a bad room doesn't use up an ID
    }

    /**
     * Creates an independent copy of a room, keeping its ID. Used for undo snapshots.
     *
     * @param original the room to copy
     */
    Room(Room original) {
        this.capacity = original.getCapacity();
        this.roomType = original.getRoomType();
        this.features = original.getFeatures();
        this.roomID = original.getRoomID();
    }

    /**
     * Returns the capacity.
     *
     * @return the number of people the room holds
     */
    public int getCapacity() { return capacity; }

    /**
     * Returns the room type.
     *
     * @return the room type
     */
    public RoomType getRoomType() { return roomType; }

    /**
     * Returns a copy of the features, so callers cannot change the stored list.
     *
     * @return the features in the order they were entered
     */
    public ArrayList<String> getFeatures() { return new ArrayList<>(features); }

    /**
     * Returns the unique room ID.
     *
     * @return the room ID
     */
    public String getRoomID() { return roomID; }

    /**
     * Updates one room field, preserving the original room ID.
     *
     * @param field roomType, capacity or features
     * @param newValue the new value of the field
     * @throws IllegalArgumentException if the field or value is invalid
     */
    void modifyField(String field, String newValue) {
        if (newValue == null) {
            throw new IllegalArgumentException("A new value is required");
        }
        switch (field) {
            case "capacity":
                try {
                    capacity = checkCapacity(Integer.parseInt(newValue));
                } catch (NumberFormatException invalidNumber) {
                    throw new IllegalArgumentException("Capacity must be an integer");
                }
                break;
            case "roomType":
                roomType = RoomType.fromString(newValue);
                break;
            case "features":
                features = parseFeatures(newValue);
                break;
            default:
                throw new IllegalArgumentException("Editable room fields: roomType, capacity, features");
        }
    }

    /**
     * Parses a comma-separated feature list. '-' or an empty string means no features;
     * feature names are case-sensitive and duplicates are removed.
     *
     * @param str the feature list typed by the user
     * @return the parsed features
     * @throws IllegalArgumentException if the list contains spaces or empty names
     */
    static ArrayList<String> parseFeatures(String str) {
        ArrayList<String> list = new ArrayList<String>();
        if (str == null || str.isEmpty() || "-".equals(str)) {
            return list;
        }
        if (str.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("Separate features with commas and no spaces");
        }
        String[] arr = str.split(",", -1);
        for (String f : arr) {
            if (f.isEmpty()) {
                throw new IllegalArgumentException("Feature names must not be empty");
            }
            if (!list.contains(f)) {
                list.add(f);
            }
        }
        return list;
    }

    private String generateRoomID(RoomType r){
        int num = RoomType.incrementRoomTypeCount(r);
        return r.getIdPrefix() + String.format("%08x", num);
    }

    private static int checkCapacity(int capacity){
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be a positive integer, got: " + capacity);
        }
        return capacity;
    }
}
