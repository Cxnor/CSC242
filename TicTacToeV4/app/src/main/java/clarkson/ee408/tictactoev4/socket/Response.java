package clarkson.ee408.tictactoev4.socket;

public class Response {
    /**
     *
     */
    public enum ResponseStatus {
        SUCCESS, FAILURE
    }

    private ResponseStatus status;
    private String message;

    /**
     *
     */
    public Response() {
    }

    /**
     *
     * @param status
     * @param message
     */
    public Response(ResponseStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    /**
     *
     * @return
     */
    public ResponseStatus getStatus() {
        return status;
    }

    /**
     *
     * @return
     */
    public String getMessage() {
        return message;
    }

    /**
     *
     * @param status
     */
    public void setStatus(ResponseStatus status) {
        this.status = status;
    }

    /**
     *
     * @param message
     */
    public void setMessage(String message) {
        this.message = message;
    }
}

