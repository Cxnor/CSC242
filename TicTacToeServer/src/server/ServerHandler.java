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
            // Save the move in the server and return a standard Response
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
        // check if there is a valid move made by my opponent
        if (event.getMove() != -1 && !event.getTurn().equals(currentUsername)){
            response.setMove(event.getMove());
            // Delete the move
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
                // Handle UPDATE_PAIRING request
                break;
            case SEND_INVITATION:
                // Handle SEND_INVITATION request
                break;
            case SEND_MOVE:
                int move = gson.fromJson(request.getData(), Integer.class);
                return handleSendMove(move);
            case REQUEST_MOVE:
                return handleRequestMove();
            case ABORT_GAME:
                // Handle ABORT_GAME request
                break;
            case COMPLETE_GAME:
                // Handle COMPLETE_GAME request
                break;
            default:
                return new Response(Response.ResponseStatus.FAILURE, "Invalid request type.");
        }
        return null; // added this to fix error it was throwing probably have to remove it.
    }

    /**
     *
     */
    public void close() {
        try {
            // Close the I/O streams
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }

            // Close the client socket
            if (clientSocket != null) {
                clientSocket.close();
            }
        } catch (IOException e) {
            // Handle any exceptions that may occur during the close operation
            e.printStackTrace();
        } finally {
            // Log useful server information (e.g., client disconnection)
            System.out.println("Client disconnected: " + currentUsername);
        }
    }
    public void run() {
        try {
            while (true) {
                // Read the serialized request from the client
                String serializedRequest = inputStream.readUTF();

                // Deserialize the request using Gson
                Request request = gson.fromJson(serializedRequest, Request.class);

                // Handle the request to get a response
                Response response = handleRequest(request);

                // Serialize the response
                String serializedResponse = gson.toJson(response);

                // Write the response to the client
                outputStream.writeUTF(serializedResponse);
                outputStream.flush();
            }
        } catch (EOFException e) {
            // Client disconnected (EOFException is thrown)
            System.out.println("Client " + currentUsername + " disconnected.");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            // Close the connection when the loop exits
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
            // Use database helper functions to get available users and invitations
            List<User> availableUsers = databaseHelper.getAvailableUsers(currentUsername);
            Event userInvitation = databaseHelper.getUserInvitation(currentUsername);
            Event userInvitationResponse = databaseHelper.getUserInvitationResponse(currentUsername);

            // Construct PairingResponse object
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






}