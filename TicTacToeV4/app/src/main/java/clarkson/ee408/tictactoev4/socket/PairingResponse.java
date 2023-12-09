package clarkson.ee408.tictactoev4.socket;
import clarkson.ee408.tictactoev4.model.*;
import java.util.List;

public class PairingResponse extends Response {

    private List<User> availableUsers;
    private Event invitation;
    private Event invitationResponse;

    /**
     * Default constructor.
     */
    public PairingResponse() {
        super();
    }

    /**
     * Constructor with all attributes.
     *
     * @param status             The response status.
     * @param message            The response message.
     * @param availableUsers     List of available users.
     * @param invitation         Event representing a game invitation.
     * @param invitationResponse Event representing a response to a game invitation.
     */
    public PairingResponse(ResponseStatus status, String message, List<User> availableUsers,
                           Event invitation, Event invitationResponse) {
        super(status, message);
        this.availableUsers = availableUsers;
        this.invitation = invitation;
        this.invitationResponse = invitationResponse;
    }

    /**
     * Get the list of available users.
     *
     * @return List of available users.
     */
    public List<User> getAvailableUsers() {
        return availableUsers;
    }

    /**
     * Set the list of available users.
     *
     * @param availableUsers List of available users.
     */
    public void setAvailableUsers(List<User> availableUsers) {
        this.availableUsers = availableUsers;
    }

    /**
     * Get the game invitation event.
     *
     * @return Event representing a game invitation.
     */
    public Event getInvitation() {
        return invitation;
    }

    /**
     * Set the game invitation event.
     *
     * @param invitation Event representing a game invitation.
     */
    public void setInvitation(Event invitation) {
        this.invitation = invitation;
    }

    /**
     * Get the invitation response event.
     *
     * @return Event representing a response to a game invitation.
     */
    public Event getInvitationResponse() {
        return invitationResponse;
    }

    /**
     * Set the invitation response event.
     *
     * @param invitationResponse Event representing a response to a game invitation.
     */
    public void setInvitationResponse(Event invitationResponse) {
        this.invitationResponse = invitationResponse;
    }
}
