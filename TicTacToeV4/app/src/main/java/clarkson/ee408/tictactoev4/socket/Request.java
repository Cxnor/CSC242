package clarkson.ee408.tictactoev4.socket;

public class Request {
    public enum RequestType {
        LOGIN, REGISTER, UPDATE_PAIRING, SEND_INVITATION, ACCEPT_INVITATION,
        DECLINE_INVITATION, ACKNOWLEDGE_RESPONSE, REQUEST_MOVE, SEND_MOVE, ABORT_GAME, COMPLETE_GAME
    }

    private RequestType type;
    private String data;

    public Request() {
    }

    /**
     *
     * @param type
     * @param data
     */
    public Request(RequestType type, String data) {
        this.type = type;
        this.data = data;
    }

    /**
     *
     * @return
     */
    public RequestType getType() {
        return type;
    }

    /**
     *
     * @return
     */
    public String getData() {
        return data;
    }

    /**
     *
     * @param type
     */
    public void setType(RequestType type) {
        this.type = type;
    }

    /**
     *
     * @param data
     */
    public void setData(String data) {
        this.data = data;
    }
}

