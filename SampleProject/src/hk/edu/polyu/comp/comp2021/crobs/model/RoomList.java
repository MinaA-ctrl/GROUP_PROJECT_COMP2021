package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** The collection of all rooms in one CROBS session. */
public class RoomList{
    private final ArrayList<Room> rooms = new ArrayList<Room>();

    /**
     * Creates and stores a new room with a generated ID.
     *
     * @param capacity the positive capacity
     * @param type the room type
     * @param features comma-separated features without spaces, or - for none
     * @return the new room
     * @throws IllegalArgumentException if any argument is invalid
     */
    public Room addRoom(int capacity, RoomType type, String features){
        Room room = new Room(capacity, type, features);
        rooms.add(room);
        return room;
    }

    /**
     * Finds a room by ID.
     *
     * @param roomID the room ID
     * @return the stored room
     * @throws IllegalArgumentException if no room has that ID
     */
    public Room getRoom(String roomID){
        for (Room room : rooms) {
            if (room.getRoomID().equals(roomID)) {
                return room;
            }
        }
        throw new IllegalArgumentException("Room not found: " + roomID);
    }

    /**
     * Removes a room by ID.
     *
     * @param roomID the room ID
     * @return the removed room
     * @throws IllegalArgumentException if no room has that ID
     */
    public Room deleteRoom(String roomID){
        Room room = getRoom(roomID);
        rooms.remove(room);
        return room;
    }

    /**
     * Checks whether a room exists.
     *
     * @param roomID the room ID
     * @return true if a room has that ID
     */
    public boolean containsRoom(String roomID) {
        for (Room room : rooms) {
            if (room.getRoomID().equals(roomID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Changes one field of a room.
     *
     * @param roomID the room ID
     * @param field roomType, capacity or features
     * @param newValue the new value
     * @return the modified room
     * @throws IllegalArgumentException if the room, field or value is invalid
     */
    public Room modifyRoom(String roomID, String field, String newValue) {
        Room room = getRoom(roomID);
        if (field == null) {
            throw new IllegalArgumentException("A room field is required");
        }
        room.modifyField(field, newValue);
        return room;
    }

    /**
     * Returns all rooms sorted in ascending order of room ID.
     *
     * @return a new sorted list of the rooms
     */
    public ArrayList<Room> getAllRooms(){
        ArrayList<Room> result = new ArrayList<>(rooms);
        result.sort(Comparator.comparing(Room::getRoomID));
        return result;
    }

    /**
     * Copies every room for an undo snapshot.
     *
     * @return independent copies of the rooms
     */
    List<Room> copyRooms() {
        List<Room> copies = new ArrayList<>();
        for (Room room : rooms) {
            copies.add(new Room(room));
        }
        return copies;
    }

    /**
     * Replaces all rooms with copies of saved ones.
     *
     * @param savedRooms the rooms to restore
     */
    void restoreRooms(List<Room> savedRooms) {
        rooms.clear();
        for (Room room : savedRooms) {
            rooms.add(new Room(room));
        }
    }
}
