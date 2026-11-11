package hk.edu.polyu.comp.comp2021.crobs.model;

public enum RoomType {
    LECTURE_THEATRE("lectureTheatre", "lt", 0),
    COMPUTER_LAB("computerLab", "cl", 1),
    SEMINAR_ROOM("seminarRoom", "sr", 2),
    MEETING_ROOM("meetingRoom", "mr", 3);

    private final String commandName;
    private final String idPrefix;
    private final int indexOfCount;
    private static int[] roomIdCount = {-1, -1, -1, -1};

    RoomType(String commandName, String idPrefix, int index){
        this.commandName = commandName;
        this.idPrefix = idPrefix;
        this.indexOfCount = index;
    }

    public static RoomType fromString(String s) {
        for (RoomType t : values()) {
            if (t.commandName.equalsIgnoreCase(s)) return t;
        }
        throw new IllegalArgumentException("Unknown room type: " + s);
    }

    public String getIdPrefix() { return idPrefix; }

    public int getIndex() { return indexOfCount; }

    @Override
    public String toString() { return commandName; }

    // IDs are not reused when rooms are deleted.
    public static int incrementRoomTypeCount(RoomType r){
        roomIdCount[r.getIndex()] += 1;
        return roomIdCount[r.getIndex()];
    }

    // puts all counters back to the start, used in unit tests
    static void resetRoomTypeCount(){
        roomIdCount = new int[]{-1, -1, -1, -1};
    }
}
