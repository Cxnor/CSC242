package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import model.Event;
import model.User;
import socket.Request;
import socket.Response;
import java.io.EOFException;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import socket.GamingResponse;
import java.util.logging.Logger;
import server.DatabaseHelper.*;
import socket.PairingResponse;


public class ServerHandler extends Thread {
    private Socket socket;
    private final Logger LOGGER;

    private String currentUsername;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Gson gson;
    private int currentEventId;

    /**
     *
     * @param socket
     * @throws IOException
     */
    public ServerHandler(Socket socket) throws IOException {
        LOGGER = Logger.getLogger(ServerHandler.class.getName());
        this.socket = socket;
        this.currentUsername = "";
        this.gson = new GsonBuilder().serializeNulls().create();
        this.inputStream = new DataInputStream(socket.getInputStream());
        this.outputStream = new DataOutputStream(socket.getOutputStream());
    }

    /**
     * Handle the SEND_MOVE request.
     *
     * @param move The move sent by the client.
     * @return Response indicating the result of the request.
     */
    private Response handleSendMove(int move){
        if(move < 0 || move > 8){ // Check for valid move
            return new Response(Response.ResponseStatus.FAILURE, "Invalid Move");
        }
        try {
            Event event = DatabaseHelper.getInstance().getEvent(currentEventId);
            if (event != null) {
                if (event.getTurn() == null || !event.getTurn().equals(currentUsername)) {
                    event.setTurn(currentUsername);
                    event.setMove(move);
                    DatabaseHelper.getInstance().updateEvent(event);
                    return new Response(Response.ResponseStatus.SUCCESS, "Move Added");
                } else {
                    return new Response(Response.ResponseStatus.FAILURE, "Not your turn to move");
                }
            } else {
                return new Response(Response.ResponseStatus.FAILURE, "Invalid Event");
            }

        }catch (SQLException e){
            return  new Response(Response.ResponseStatus.FAILURE, "Database Error");
        }
    }

    /**
     *
     * @return
     */
    private GamingResponse handleRequestMove() {
        GamingResponse response = new GamingResponse();
        try {
            if (currentEventId != -1) {
                Event event = DatabaseHelper.getInstance().getEvent(currentEventId);
                if (event.getMove() != -1 && !event.getTurn().equals(currentUsername)) {
                    response.setMove(event.getMove());
                    event.setMove(-1);
                    event.setTurn(null);
                    DatabaseHelper.getInstance().updateEvent(event);
                    response.setActive(true);
                } else {
                    response.setMove(-1);
                    if (event.getStatus() == Event.EventStatus.ABORTED) {
                        response.setActive(false);
                        response.setMessage("Opponent Abort");
                    } else if (event.getStatus() == Event.EventStatus.COMPLETED) {
                        response.setActive(false);
                        response.setMessage("Opponent Deny Play Again");
                    } else {
                        response.setActive(true);
                    }
                }
            } else {
                response.setMove(-1);
                response.setActive(false);
                response.setMessage("Invalid Event");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while handling request move", e);
            response.setStatus(Response.ResponseStatus.FAILURE);
            response.setActive(false);
            response.setMessage("Database Error");
        }

        return response;
    }

    /**
     *
     * @param request
     * @return
     */
    private Response handleRequest(Request request) {
        switch (request.getType()) {
            case LOGIN:
                User loginUser = gson.fromJson(request.getData(), User.class);
                return handleLogin(loginUser);

            case REGISTER:
                User registerUser = gson.fromJson(request.getData(), User.class);
                return handleRegister(registerUser);

            case UPDATE_PAIRING:
                return handleUpdatePairing();

            case SEND_INVITATION:
                String opponent = gson.fromJson(request.getData(), String.class);
                return handleSendInvitation(opponent);

            case ACCEPT_INVITATION:
                int acceptEventId = gson.fromJson(request.getData(), Integer.class);
                return handleAcceptInvitation(acceptEventId);

            case DECLINE_INVITATION:
                int declineEventId = gson.fromJson(request.getData(), Integer.class);
                return handleDeclineInvitation(declineEventId);

            case ACKNOWLEDGE_RESPONSE:
                int acknowledgeEventId = gson.fromJson(request.getData(), Integer.class);
                return handleAcknowledgeResponse(acknowledgeEventId);

            case REQUEST_MOVE:
                return handleRequestMove();

            case SEND_MOVE:
                int move = gson.fromJson(request.getData(), Integer.class);
                return handleSendMove(move);

            case ABORT_GAME:
                return handleAbortGame();

            case COMPLETE_GAME:
                return handleCompleteGame();

            default:
                return new Response(Response.ResponseStatus.FAILURE, "Invalid Request");
        }
    }

    /**
     *
     */
    private void close() {
        try {
            if (currentUsername != null) {
                User user = DatabaseHelper.getInstance().getUser(currentUsername);
                user.setOnline(false);
                DatabaseHelper.getInstance().updateUser(user);
                DatabaseHelper.getInstance().abortAllUserEvents(currentUsername);
            }
            inputStream.close();
            outputStream.close();
        } catch (IOException | SQLException e) {
            LOGGER.log(Level.SEVERE, "Server Info: Unable to close socket or update user status", e);
        }
    }

    /**
     *
     */
    public void run() {
        while (true) {
            try {
                String serializedRequest = inputStream.readUTF();
                Request request = gson.fromJson(serializedRequest, Request.class);
                LOGGER.log(Level.INFO, "Client Request: " + currentUsername + " - " + request.getType());

                Response response = handleRequest(request);
                String serializedResponse = gson.toJson(response);
                outputStream.writeUTF(serializedResponse);
                outputStream.flush();
            } catch (EOFException e) {
                LOGGER.log(Level.INFO, "Server Info: Client Disconnected: " + currentUsername + " - " + socket.getRemoteSocketAddress());
                close();
                break;
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Server Info: Client Connection Failed", e);
            } catch (JsonSyntaxException e) {
                LOGGER.log(Level.SEVERE, "Server Info: Serialization Error", e);
            }
        }
    }


    /**
     * Handle the REGISTER request.
     *
     * @param user The User object containing registration information.
     * @return Response indicating the result of the registration request.
     */
    private Response handleRegister(User user){
        try {
            if (DatabaseHelper.getInstance().isUsernameExists(user.getUsername())) {
                return new Response(Response.ResponseStatus.FAILURE, "Username already exists");
            } else {
                DatabaseHelper.getInstance().createUser(user);
                return new Response(Response.ResponseStatus.SUCCESS, "Registration was successful");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error while handling registration", e);
            return new Response(Response.ResponseStatus.FAILURE, "Error during registration");
        }
    }

    /**
     * Handle the LOGIN request.
     *
     * @param user The User object containing login credentials.
     * @return Response indicating the result of the login request.
     */
    private Response handleLogin(User user) {
        try {
            User storedUser = DatabaseHelper.getInstance().getUser(user.getUsername());
            if (storedUser == null){
                return new Response(Response.ResponseStatus.FAILURE, "User not found");
            }
            if (!storedUser.getPassword().equals(user.getPassword())) {
                return new Response(Response.ResponseStatus.FAILURE, "Wrong Password");
            }
            currentUsername = storedUser.getUsername();
            storedUser.setOnline(true);
            DatabaseHelper.getInstance().updateUser(storedUser);
            return new Response(Response.ResponseStatus.SUCCESS, "Login successful");

            } catch (SQLException e) {
            return new Response(Response.ResponseStatus.FAILURE, "Database Error");
        }
    }

    /**
     *
     * @return
     */
    public PairingResponse handleUpdatePairing() {
        // Check if a user is logged in
        if (currentUsername == null || currentUsername.isEmpty()) {
            return new PairingResponse(Response.ResponseStatus.FAILURE, "User not logged in", null, null, null);
        }

        try {
            List<User> availableUsers = DatabaseHelper.getInstance().getAvailableUsers(currentUsername);
            Event userInvitation = DatabaseHelper.getInstance().getUserInvitation(currentUsername);
            Event userInvitationResponse = DatabaseHelper.getInstance().getUserInvitationResponse(currentUsername);
            PairingResponse pairingResponse = new PairingResponse(
                    Response.ResponseStatus.SUCCESS,
                    "Pairing information retrieved successfully",
                    availableUsers,
                    userInvitation,
                    userInvitationResponse
            );

            return pairingResponse;
        } catch (SQLException e) {
            e.printStackTrace();
            return new PairingResponse(Response.ResponseStatus.FAILURE, "Error retrieving pairing information", null, null, null);
        }
    }

    /**
     *
     * @param opponent
     * @return
     */
    public Response handleSendInvitation(String opponent) {
        if (currentUsername == null || currentUsername.isEmpty()) {
            return new Response(Response.ResponseStatus.FAILURE, "User not logged in");
        }
        try {
            if (!DatabaseHelper.getInstance().isUserAvailable(opponent)) {
                return new Response(Response.ResponseStatus.FAILURE, "Opponent not available for invitation");
            }
            Event invitationEvent = new Event(currentUsername, opponent, Event.EventStatus.PENDING, null, -1);
            DatabaseHelper.getInstance().createEvent(invitationEvent);
            return new Response(Response.ResponseStatus.SUCCESS, "Invitation sent successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.ResponseStatus.FAILURE, "Error sending invitation");
        }
    }

    /**
     *
     * @param eventId
     * @return
     */
    public Response handleAcceptInvitation(int eventId) {

        try {
            Event invitationEvent = DatabaseHelper.getInstance().getEvent(eventId);

            if (invitationEvent.getStatus() != Event.EventStatus.PENDING || invitationEvent == null || !invitationEvent.getOpponent().equals(currentUsername)) {
                return new Response(Response.ResponseStatus.FAILURE, "Invalid invitation");
            }
            invitationEvent.setStatus(Event.EventStatus.ACCEPTED);
            DatabaseHelper.getInstance().abortAllUserEvents(currentUsername);
            DatabaseHelper.getInstance().updateEvent(invitationEvent);
            currentEventId = eventId;
            return new Response(Response.ResponseStatus.SUCCESS, "Invitation accepted successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.ResponseStatus.FAILURE, "Error accepting invitation");
        }
    }

    /**
     *
     * @param eventId
     * @return
     */
    private Response handleDeclineInvitation(int eventId) {
        try {
            Event invitationEvent = DatabaseHelper.getInstance().getEvent(eventId);
            if (invitationEvent == null || invitationEvent.getStatus() != Event.EventStatus.PENDING || !invitationEvent.getOpponent().equals(currentUsername)) {
                return new Response(Response.ResponseStatus.FAILURE, "Invalid invitation or invitation already accepted/declined");
            }
            invitationEvent.setStatus(Event.EventStatus.DECLINED);
            DatabaseHelper.getInstance().updateEvent(invitationEvent);
            return new Response(Response.ResponseStatus.SUCCESS, "Invitation declined successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.ResponseStatus.FAILURE, "Error declining invitation");
        }
    }

    /**
     *
     * @param eventId
     * @return
     */
    private Response handleAcknowledgeResponse(int eventId) {
        try {
            Event invitationEvent = DatabaseHelper.getInstance().getEvent(eventId);
            if (invitationEvent == null || !invitationEvent.getSender().equals(currentUsername)) {
                return new Response(Response.ResponseStatus.FAILURE, "Invalid event or not the sender");
            }
            if (invitationEvent.getStatus() == Event.EventStatus.PENDING) {
                return new Response(Response.ResponseStatus.FAILURE, "Response not received yet");
            }
            if (invitationEvent.getStatus() == Event.EventStatus.DECLINED) {
                invitationEvent.setStatus(Event.EventStatus.ABORTED);
            } else if (invitationEvent.getStatus() == Event.EventStatus.ACCEPTED) {
                currentEventId = eventId;
                DatabaseHelper.getInstance().abortAllUserEvents(currentUsername);
            }
            DatabaseHelper.getInstance().updateEvent(invitationEvent);
            return new Response(Response.ResponseStatus.SUCCESS, "Response acknowledged successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.ResponseStatus.FAILURE, "Error acknowledging response");
        }
    }

    /**
     *
     * @return
     */
    private Response handleCompleteGame() {
        try {
            Event playingEvent = DatabaseHelper.getInstance().getEvent(currentEventId);
            if (playingEvent == null || playingEvent.getStatus() != Event.EventStatus.PLAYING) {
                return new Response(Response.ResponseStatus.FAILURE, "No game in progress");
            }

            playingEvent.setStatus(Event.EventStatus.COMPLETED);
            DatabaseHelper.getInstance().updateEvent(playingEvent);
            currentEventId = -1;

            return new Response(Response.ResponseStatus.SUCCESS, "Game completed successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.ResponseStatus.FAILURE, "Error completing game");
        }
    }

    /**
     *
     * @return
     */
    private Response handleAbortGame() {
        try {
            Event playingEvent = DatabaseHelper.getInstance().getEvent(currentEventId); // Retrieve the event by currentEventId
            if (playingEvent == null || playingEvent.getStatus() != Event.EventStatus.PLAYING) {
                return new Response(Response.ResponseStatus.FAILURE, "No game in progress");
            }

            playingEvent.setStatus(Event.EventStatus.ABORTED);
            DatabaseHelper.getInstance().updateEvent(playingEvent);
            currentEventId = -1;

            return new Response(Response.ResponseStatus.SUCCESS, "Game aborted successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.ResponseStatus.FAILURE, "Error aborting game");
        }
    }
}