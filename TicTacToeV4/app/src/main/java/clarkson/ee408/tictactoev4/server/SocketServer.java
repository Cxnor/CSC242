package clarkson.ee408.tictactoev4.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer {
    private final int PORT;

    public SocketServer() throws Exception {
        this(5000);
    }

    public SocketServer(int port) throws Exception {
        if (port < 0) {
            throw new Exception("Port number cannot be negative");
        }
        PORT = port;
    }

    public static SocketServer setup() throws Exception {
        return new SocketServer();
    }

    public void startAcceptingRequests() {
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server is listening on port " + PORT);

            int clientCounter = 0;
            int maxClients = 2;

            while (true) {
                if (clientCounter < maxClients) {
                    Socket clientSocket = serverSocket.accept();
                    clientCounter++;

                    String username = "User" + clientCounter;

                    // Create a new ServerHandler instance and start a separate thread for each client
                    ServerHandler handler = new ServerHandler(clientSocket, username);
                    handler.start();

                    System.out.println("Client " + username + " connected.");
                } else {
                    // Optionally reject additional connections if you have reached the limit
                    System.out.println("Connection limit reached. Rejecting new connections.");
                    // Close the new socket or take other appropriate action
                    Socket rejectedSocket = serverSocket.accept();
                    rejectedSocket.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return PORT;
    }

    public static void main(String[] args) {
        try {
            SocketServer socketServer = setup();
            socketServer.startAcceptingRequests();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
