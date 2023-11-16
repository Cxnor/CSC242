package model;

public class Event {
    public enum EventStatus {
        PENDING, DECLINED, ACCEPTED, PLAYING, COMPLETED, ABORTED
    }

    private int eventId;
    private String sender;
    private String opponent;
    private EventStatus status;
    private String turn;
    private int move;

    public Event() {
        // Default constructor
    }

    public Event(int eventId, String sender, String opponent, EventStatus status, String turn, int move) {
        this.eventId = eventId;
        this.sender = sender;
        this.opponent = opponent;
        this.status = status;
        this.turn = turn;
        this.move = move;
    }
    public Event(String sender, String opponent, String turn) {
        this.sender = sender;
        this.opponent = opponent;
        this.status = EventStatus.PENDING; // Default status
        this.turn = turn;
    }


    // Getters
    public int getEventId() {
        return eventId;
    }

    public String getSender() {
        return sender;
    }

    public String getOpponent() {
        return opponent;
    }

    public EventStatus getStatus() {
        return status;
    }

    public String getTurn() {
        return turn;
    }

    public int getMove() {
        return move;
    }

    // Setters
    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setOpponent(String opponent) {
        this.opponent = opponent;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public void setTurn(String turn) {
        this.turn = turn;
    }

    public void setMove(int move) {
        this.move = move;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Event otherEvent = (Event) obj;
        return eventId == otherEvent.eventId;
    }
}
