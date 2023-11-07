package clarkson.ee408.tictactoev4.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import clarkson.ee408.tictactoev4.model.Event;
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
            this.gameEvent = new Event();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param move
     * @return
     */
    private Response handleSendMove(int move) {
        if (gameEvent.getTurn().equals(currentUsername)) {
            gameEvent.setMove(move);
            gameEvent.setTurn(gameEvent.getOpponent());
            return new Response(Response.ResponseStatus.SUCCESS, "Move sent successfully.");

        } else {
            return new Response(Response.ResponseStatus.FAILURE, "It's not your turn to make a move.");
        }
    }

    private Response handleRequestMove() {
        int opponentMove = gameEvent.getMove();
        if (opponentMove != -1) {
            String move = Integer.toString(opponentMove);
            gameEvent.setMove(-1);
            return new Response(Response.ResponseStatus.SUCCESS, move);
        } else {
            return new Response(Response.ResponseStatus.SUCCESS, "-1");
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
                return handleSendMove(gameEvent.getMove());
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
                String serializedRequest = inputStream.readUTF();
                Request request = gson.fromJson(serializedRequest, Request.class);
                Response response = handleRequest(request);
                String serializedResponse = gson.toJson(response);
                outputStream.writeUTF(serializedResponse);
                outputStream.flush();
            }
        } catch (EOFException e) {
            System.out.println("Client " + currentUsername + " disconnected.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

}