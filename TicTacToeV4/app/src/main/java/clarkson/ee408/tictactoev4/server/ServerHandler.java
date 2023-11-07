package clarkson.ee408.tictactoev4.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Event;
import clarkson.ee408.tictactoev4.socket.*;
import java.io.EOFException;


public class ServerHandler extends Thread {
    private Socket clientSocket;
    private String currentUsername;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Gson gson; // Gson attribute for JSON serialization
    private static Event gameEvent;
    /**
     * Constructor to initialize the client socket, username, I/O streams, and Gson.
     *
     * @param clientSocket The client's socket.
     * @param username The username to identify the user.
     */
    public ServerHandler(Socket clientSocket, String username) {
        this.clientSocket = clientSocket;
        this.currentUsername = username;

        try {
            // Initialize the input and output streams
            this.inputStream = new DataInputStream(clientSocket.getInputStream());
            this.outputStream = new DataOutputStream(clientSocket.getOutputStream());
            this.gson = new GsonBuilder().serializeNulls().create();
            this.gameEvent = new Event("senderValue", "opponentValue", "turnValue");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handle the SEND_MOVE request.
     *
     * @param move The move sent by the client.
     * @return Response indicating the result of the request.
     */
    private Response handleSendMove(String move) {
        // Check if the last move was made by the user (assuming the user is the sender)
        if (gameEvent.getTurn().equals(currentUsername)) {
            // Set the move and change the turn
            gameEvent.setMove(move);
            gameEvent.setTurn(gameEvent.getOpponent());

            return new Response(Response.ResponseStatus.SUCCESS, "Move sent successfully.");
        } else {
            // The user can't make consecutive moves, return an error response
            return new Response(Response.ResponseStatus.FAILURE, "It's not your turn to make a move.");
        }
    }

    /**
     * Handle the REQUEST_MOVE request.
     *
     * @return Response containing the opponent's move or indicating no move if not available.
     */
    private Response handleRequestMove() {
        String opponentMove = gameEvent.getMove();

        // Check if there's a valid move made by the opponent
        if (opponentMove != null && !opponentMove.isEmpty()) {
            // Delete the move from the event
            gameEvent.setMove(null);
            return new Response(Response.ResponseStatus.SUCCESS, opponentMove);
        } else {
            return new Response(Response.ResponseStatus.SUCCESS, "-1"); // No move made by the opponent
        }
    }

    /**
     * Handle a general request.
     *
     * @param request The Request object received from the client.
     * @return Response indicating the result of the request.
     */
    public Response handleRequest(Request request) {
        switch (request.getType()) {
            case SEND_MOVE:
                return handleSendMove(request.getData());
            case REQUEST_MOVE:
                return handleRequestMove();
            default:
                return new Response(Response.ResponseStatus.FAILURE, "Invalid request type.");
        }
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
        } finally {
            // Close the connection when the loop exits
            close();
        }
    }

}