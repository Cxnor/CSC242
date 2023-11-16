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
import java.util.logging.Logger;


public class ServerHandler extends Thread {
    private Socket clientSocket;
    private final Logger LOGGER;

    private String currentUsername;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Gson gson; // Gson attribute for JSON serialization
    private static Event gameEvent;
    public static Event event = new Event(1, null, null, null, null, -1);

    /**
     *
     * @param socket
     * @param username
     * @throws IOException
     */
    public ServerHandler(Socket socket, String username) throws IOException {
        LOGGER = Logger.getLogger(ServerHandler.class.getName());

        this.clientSocket = socket;
        this.currentUsername = username;
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
    private Response handleSendMove(int move) {
        if(move < 0 || move > 8){ // Check for valid move
            return new Response(Response.ResponseStatus.FAILURE, "Invalid Move");
        }
        if(event.getTurn() == null || !event.getTurn().equals(currentUsername)) {
            // Save the move in the server and return a standard Response
            event.setMove(move);
            event.setTurn(currentUsername);
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
    private GamingResponse handleRequestMove() {
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
        return response;
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
                int move = gson.fromJson(request.getData(), Integer.class);
                return handleSendMove(move);
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