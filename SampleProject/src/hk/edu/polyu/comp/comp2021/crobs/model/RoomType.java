package hk.edu.polyu.comp.comp2021.crobs.model;

/** The supported room types, each with its command name and room ID prefix. */
public enum RoomType {
    /** A lecture theatre, with IDs starting with lt. */
    LECTURE_THEATRE("lectureTheatre", "lt", 0),
    /** A computer lab, with IDs starting with cl. */
    COMPUTER_LAB("computerLab", "cl", 1),
    /** A seminar room, with IDs starting with sr. */
    SEMINAR_ROOM("seminarRoom", "sr", 2),
    /** A meeting room, with IDs starting with mr. */
    MEETING_ROOM("meetingRoom", "mr", 3);

    private final String commandName;
    private final String idPrefix;
    private final int indexOfCount;
    private static int[] roomIdCount = {-1, -1, -1, -1};

    /**
     * Creates a room type constant.
     *
     * @param commandName the name used on the command line
     * @param idPrefix the two-letter room ID prefix
     * @param index the index of this type's ID counter
     */
    RoomType(String commandName, String idPrefix, int index){
        this.commandName = commandName;
        this.idPrefix = idPrefix;
        this.indexOfCount = index;
    }

    /**
     * Converts a command-line room type name, ignoring case.
     *
     * @param s the room type name, e.g. lectureTheatre
     * @return the matching room type
     * @throws IllegalArgumentException if no room type has that name
     */
    public static RoomType fromString(String s) {
        for (RoomType t : values()) {
            if (t.toString().equalsIgnoreCase(s)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown room type: " + s);
    }

    /**
     * Returns the two-letter prefix of room IDs of this type.
     *
     * @return the ID prefix
     */
    public String getIdPrefix() { return idPrefix; }

    /**
     * Returns the index of this type's ID counter.
     *
     * @return the counter index
     */
    public int getIndex() { return indexOfCount; }

    /**
     * Returns the name used for this type on the command line.
     *
     * @return the command name
     */
    @Override
    public String toString() { return commandName; }

    /**
     * Advances the ID counter of a room type. IDs are not reused when rooms are deleted
     * or when adding a room is undone.
     *
     * @param r the room type
     * @return the next number for that type
     */
    public static int incrementRoomTypeCount(RoomType r){
        roomIdCount[r.getIndex()] += 1;
        return roomIdCount[r.getIndex()];
    }

    /** Puts all counters back to the start; used in unit tests. */
    static void resetRoomTypeCount(){
        roomIdCount = new int[]{-1, -1, -1, -1};
    }
}
