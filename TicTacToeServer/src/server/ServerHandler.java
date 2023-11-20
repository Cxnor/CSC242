package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    private Socket clientSocket;
    private final Logger LOGGER;

    private String currentUsername;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Gson gson; // Gson attribute for JSON serialization
    private int currentEventId;
    private DatabaseHelper databaseHelper; // Add this member variable

    /**
     *
     * @param socket
     * @throws IOException
     */
    public ServerHandler(Socket socket) throws IOException {
        LOGGER = Logger.getLogger(ServerHandler.class.getName());

        this.clientSocket = socket;
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
    private Response handleSendMove(int move) throws SQLException {
        Event event = databaseHelper.getEvent(currentEventId);
        if(move < 0 || move > 8){ // Check for valid move
            return new Response(Response.ResponseStatus.FAILURE, "Invalid Move");
        }
        if(event.getTurn() == null || !event.getTurn().equals(currentUsername)) {
            event.setMove(move);
            event.setTurn(currentUsername);
            databaseHelper.updateEvent(event);
            return new Response(Response.ResponseStatus.SUCCESS, "Move Added");
        }else{
            return new Response(Response.ResponseStatus.FAILURE, "Not your turn to move");
        }
    }

    /**
     * Handle the REQUEST_MOVE request.
     *
     * @return Response containing the opponent's move or indicating no move if not available.
     */
    private GamingResponse handleRequestMove() throws SQLException {
        Event event = databaseHelper.getEvent(currentEventId);
        GamingResponse response = new GamingResponse();
        response.setStatus(Response.ResponseStatus.SUCCESS);
        if (event.getMove() != -1 && !event.getTurn().equals(currentUsername)){
            response.setMove(event.getMove());
            event.setMove(-1);
            event.setTurn(null);
        }else{
            response.setMove(-1);
        }
        databaseHelper.updateEvent(event);
        return response;
    }

    /**
     *
     * @param request
     * @return
     */
    public Response handleRequest(Request request) throws SQLException {
        switch (request.getType()) {
            case LOGIN:
                User user_login = gson.fromJson(request.getData(), User.class);
                return handleLogin(user_login);
            case REGISTER:
                User user_register = gson.fromJson(request.getData(), User.class);
                return handleRegister(user_register);
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
            case SEND_MOVE:
                int move = gson.fromJson(request.getData(), Integer.class);
                return handleSendMove(move);
            case REQUEST_MOVE:
                return handleRequestMove();
            case ABORT_GAME:
                return handleAbortGame();
            case COMPLETE_GAME:
                return handleCompleteGame();
            default:
                return new Response(Response.ResponseStatus.FAILURE, "Invalid request type.");
        }
    }


    public void close() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (clientSocket != null) {
                clientSocket.close();
            }
            if (currentUsername != null && !currentUsername.isEmpty()) {
                User user = databaseHelper.getUser(currentUsername);
                user.setOnline(false);
                databaseHelper.updateUser(user);
                databaseHelper.abortAllUserEvents(currentUsername);
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } finally {
            System.out.println("Client disconnected: " + currentUsername);
        }
    }

    public void run() {
        try {
            while (true) {
                String serializedRequest = inputStream.readUTF();
                Request request = gson.fromJson(serializedRequest, Request.class);
                Response response = handleRequest(request);
                String serializedResponse = gson.toJson(response);
                outputStream.writeUTF(serializedResponse);
                outputStream.flush();
            }
        } catch (EOFException e) {
            System.out.println("Client " + currentUsername + " disconnected due to EOFException.");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }


    /**
     * Handle the REGISTER request.
     *
     * @param user The User object containing registration information.
     * @return Response indicating the result of the registration request.
     */
    private Response handleRegister(User user) throws SQLException {
        if (databaseHelper.isUsernameExists(user.getUsername())) {
            return new Response(Response.ResponseStatus.FAILURE, "Username already exists");
        }
        try {
            databaseHelper.createUser(user);
            return new Response(Response.ResponseStatus.SUCCESS, "Registration successful");
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.ResponseStatus.FAILURE, "Error during registration");
        }
    }

    /**
     * Handle the LOGIN request.
     *
     * @param user The User object containing login credentials.
     * @return Response indicating the result of the login request.
     */
    private Response handleLogin(User user) throws SQLException {
        User storedUser = databaseHelper.getUser(user.getUsername());
        if (storedUser != null && storedUser.getPassword().equals(user.getPassword())) {
            currentUsername = user.getUsername();
            storedUser.setOnline(true);
            databaseHelper.updateUser(storedUser);
            return new Response(Response.ResponseStatus.SUCCESS, "Login successful");
        } else {
            return new Response(Response.ResponseStatus.FAILURE, "Invalid username or password");
        }
    }

    public PairingResponse handleUpdatePairing() {
        // Check if a user is logged in
        if (currentUsername == null || currentUsername.isEmpty()) {
            return new PairingResponse(Response.ResponseStatus.FAILURE, "User not logged in", null, null, null);
        }

        try {
            List<User> availableUsers = databaseHelper.getAvailableUsers(currentUsername);
            Event userInvitation = databaseHelper.getUserInvitation(currentUsername);
            Event userInvitationResponse = databaseHelper.getUserInvitationResponse(currentUsername);
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

    public Response handleSendInvitation(String opponent) {
        if (currentUsername == null || currentUsername.isEmpty()) {
            return new Response(Response.ResponseStatus.FAILURE, "User not logged in");
        }
        try {
            if (!databaseHelper.isUserAvailable(opponent)) {
                return new Response(Response.ResponseStatus.FAILURE, "Opponent not available for invitation");
            }
            Event invitationEvent = new Event(currentUsername, opponent, Event.EventStatus.PENDING, null, -1);
            databaseHelper.createEvent(invitationEvent);
            return new Response(Response.ResponseStatus.SUCCESS, "Invitation sent successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.ResponseStatus.FAILURE, "Error sending invitation");
        }
    }

    public Response handleAcceptInvitation(int eventId) {
        if (currentUsername == null || currentUsername.isEmpty()) {
            return new Response(Response.ResponseStatus.FAILURE, "User not logged in");
        }
        try {
            Event invitationEvent = databaseHelper.getEvent(eventId);
            if (invitationEvent == null || invitationEvent.getStatus() != Event.EventStatus.PENDING) {
                return new Response(Response.ResponseStatus.FAILURE, "Invalid invitation or invitation already accepted/declined");
            }
            if (!invitationEvent.getOpponent().equals(currentUsername)) {
                return new Response(Response.ResponseStatus.FAILURE, "Invalid invitation for the current user");
            }
            invitationEvent.setStatus(Event.EventStatus.ACCEPTED);
            databaseHelper.abortAllUserEvents(currentUsername);
            databaseHelper.updateEvent(invitationEvent);
            currentEventId = eventId;
            return new Response(Response.ResponseStatus.SUCCESS, "Invitation accepted successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.ResponseStatus.FAILURE, "Error accepting invitation");
        }
    }

    private Response handleDeclineInvitation(int eventId) {
        if (currentUsername == null || currentUsername.isEmpty()) {
            return new Response(Response.ResponseStatus.FAILURE, "User not logged in");
        }
        try {
            Event invitationEvent = databaseHelper.getEvent(eventId);
            if (invitationEvent == null || invitationEvent.getStatus() != Event.EventStatus.PENDING) {
                return new Response(Response.ResponseStatus.FAILURE, "Invalid invitation or invitation already accepted/declined");
            }
            if (!invitationEvent.getOpponent().equals(currentUsername)) {
                return new Response(Response.ResponseStatus.FAILURE, "Invalid invitation for the current user");
            }
            invitationEvent.setStatus(Event.EventStatus.DECLINED);
            databaseHelper.updateEvent(invitationEvent);
            return new Response(Response.ResponseStatus.SUCCESS, "Invitation declined successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.ResponseStatus.FAILURE, "Error declining invitation");
        }
    }

    private Response handleAcknowledgeResponse(int eventId) {
        if (currentUsername == null || currentUsername.isEmpty()) {
            return new Response(Response.ResponseStatus.FAILURE, "User not logged in");
        }
        try {
            Event invitationEvent = databaseHelper.getEvent(eventId);
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
                databaseHelper.abortAllUserEvents(currentUsername);
            }
            databaseHelper.updateEvent(invitationEvent);
            return new Response(Response.ResponseStatus.SUCCESS, "Response acknowledged successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.ResponseStatus.FAILURE, "Error acknowledging response");
        }
    }

    // Add these methods in your ServerHandler class
    private Response handleCompleteGame() {
        // Check if a user is logged in
        if (currentUsername == null || currentUsername.isEmpty()) {
            return new Response(Response.ResponseStatus.FAILURE, "User not logged in");
        }

        try {
            Event playingEvent = databaseHelper.getEvent(currentEventId); // Retrieve the event by currentEventId
            if (playingEvent == null || playingEvent.getStatus() != Event.EventStatus.PLAYING) {
                return new Response(Response.ResponseStatus.FAILURE, "No game in progress");
            }

            playingEvent.setStatus(Event.EventStatus.COMPLETED);
            databaseHelper.updateEvent(playingEvent);
            currentEventId = -1;

            return new Response(Response.ResponseStatus.SUCCESS, "Game completed successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.ResponseStatus.FAILURE, "Error completing game");
        }
    }

    private Response handleAbortGame() {
        // Check if a user is logged in
        if (currentUsername == null || currentUsername.isEmpty()) {
            return new Response(Response.ResponseStatus.FAILURE, "User not logged in");
        }

        try {
            Event playingEvent = databaseHelper.getEvent(currentEventId); // Retrieve the event by currentEventId
            if (playingEvent == null || playingEvent.getStatus() != Event.EventStatus.PLAYING) {
                return new Response(Response.ResponseStatus.FAILURE, "No game in progress");
            }

            playingEvent.setStatus(Event.EventStatus.ABORTED);
            databaseHelper.updateEvent(playingEvent);
            currentEventId = -1;

            return new Response(Response.ResponseStatus.SUCCESS, "Game aborted successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            return new Response(Response.ResponseStatus.FAILURE, "Error aborting game");
        }
    }



}