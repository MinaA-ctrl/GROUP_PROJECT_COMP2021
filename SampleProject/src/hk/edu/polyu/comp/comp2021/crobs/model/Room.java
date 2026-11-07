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
        this.roomType = type;
        this.features = assignFeatures(features);
        this.roomID = generateRoomID(type); // last, so a bad room doesn't use up an ID
    }

    public int getCapacity() { return capacity; }

    public RoomType getRoomType() { return roomType; }

    public ArrayList<String> getFeatures() { return features; }

    public String getRoomID() { return roomID; }

    private ArrayList<String> assignFeatures(String str){ //separated by comma
        ArrayList<String> list = new ArrayList<String>();
        if (str == null) {
            return list;
        }
        String[] arr = str.split(",");
        for (String f : arr) {
            if (!f.isEmpty() && !list.contains(f)) {
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
