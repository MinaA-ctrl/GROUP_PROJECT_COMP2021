package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.ArrayList;

public class Room {
    private int capacity;
    private RoomType roomType;
    private ArrayList<String> features = new ArrayList<String>();
    private final String roomID;

    Room(int capacity, RoomType type){
        this(capacity, type, "");
    }

    Room(int capacity, RoomType type, String features){
        this.capacity = checkCapacity(capacity);
        if (type == null) {
            throw new IllegalArgumentException("Room type is required");
        }
        this.roomType = type;
        this.features = parseFeatures(features);
        this.roomID = generateRoomID(type); // last, so a bad room doesn't use up an ID
    }

    public int getCapacity() { return capacity; }

    public RoomType getRoomType() { return roomType; }

    public ArrayList<String> getFeatures() { return new ArrayList<>(features); }

    public String getRoomID() { return roomID; }

    /** This updates one room field, preserving the original room ID. */
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

    // Uses '-' or an empty string for no features; feature names are case-sensitive.
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
