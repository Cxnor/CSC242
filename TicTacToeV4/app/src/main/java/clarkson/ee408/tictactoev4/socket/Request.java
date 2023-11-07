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

    public Request(RequestType requestType) {
        this.type = requestType;
    }


    public Request(RequestType type, String data) {
        this.type = type;
        this.data = data;
    }

    public RequestType getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Boolean isRequestValid() {
        for (RequestType type : RequestType.values()) {
            if (type == this.getType()) {
                return true;
            }
        }
        return false;
    }

}

