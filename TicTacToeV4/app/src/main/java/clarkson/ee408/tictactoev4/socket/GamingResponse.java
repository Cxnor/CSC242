package clarkson.ee408.tictactoev4.socket;

public class GamingResponse {
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
     * @param move
     * @param active
     */
    public GamingResponse(int move, boolean active) {
        // Call the superclass constructor if needed
        super();

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
