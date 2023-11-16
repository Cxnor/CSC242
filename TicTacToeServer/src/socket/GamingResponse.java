package socket;

public class GamingResponse extends Response {
    private int move;
    private boolean active;

    /**
     *
     */
    public GamingResponse() {
        // Default constructor, make sure to call the superclass constructor if needed
        super();
    }

    /**
     *
     * @param status
     * @param message
     * @param move
     * @param active
     */
    public GamingResponse(ResponseStatus status, String message, int move, boolean active) {
        super(status, message);
        this.move = move;
        this.active = active;
    }

    /**
     *
     * @return
     */
    public int getMove() {
        return move;
    }

    /**
     *
     * @return
     */
    public boolean isActive() {
        return active;
    }

    /**
     *
     * @param move
     */
    /**
     *
     * @param move
     */
    public void setMove(int move) {
        this.move = move;
    }

    /**
     *
     * @param active
     */
    public void setActive(boolean active) {
        this.active = active;
    }
}
