package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.ArrayList;
import java.util.Comparator;

public class RoomList{
    private final ArrayList<Room> rooms = new ArrayList<Room>();

    public Room addRoom(int capacity, RoomType type, String features){
        Room room = new Room(capacity, type, features);
        rooms.add(room);
        return room;
    }

    public Room getRoom(String roomID){
        for (Room room : rooms) {
            if (room.getRoomID().equals(roomID)) {
                return room;
            }
        }
        throw new IllegalArgumentException("Room not found: " + roomID);
    }

    public Room deleteRoom(String roomID){
        Room room = getRoom(roomID);
        rooms.remove(room);
        room.getRoomType().deleteRoomID(); //to delete now free space of the id
        return room;
    }

    public boolean containsRoom(String roomID) {
        for (Room room : rooms) {
            if (room.getRoomID().equals(roomID)) {
                return true;
            }
        }
        return false;
    }

    public Room modifyRoom(String roomID, String field, String newValue) {
        Room room = getRoom(roomID);
        if (field == null) {
            throw new IllegalArgumentException("A room field is required");
        }
        room.modifyField(field, newValue);
        return room;
    }

    public ArrayList<Room> getAllRooms(){
        ArrayList<Room> result = new ArrayList<>(rooms);
        result.sort(Comparator.comparing(Room::getRoomID));
        return result;
    }
}
