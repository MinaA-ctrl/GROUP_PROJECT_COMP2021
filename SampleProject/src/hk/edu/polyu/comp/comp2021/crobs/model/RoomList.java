package hk.edu.polyu.comp.comp2021.crobs.model;

import java.util.ArrayList;

public class RoomList{
    private ArrayList<Room> rooms = new ArrayList<Room>();

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
        return room;
    }

    public ArrayList<Room> getAllRooms(){
        return rooms;
    }
}
