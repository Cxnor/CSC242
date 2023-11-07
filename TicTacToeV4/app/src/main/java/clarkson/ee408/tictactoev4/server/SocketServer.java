package clarkson.ee408.tictactoev4.server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
public class SocketServer {
    private static final int PORT = 5000;

    /**
     *
     */
    public SocketServer() {
        // Call the parameterized constructor with the default port value of 5000
        this(PORT);
    }

    /**
     *
     * @param port
     */
    public SocketServer(int port) {
        // Constructor that sets the constant port number
        // You can use this constructor to set a custom port if needed
    }

    /**
     *
     */
    public void setup() {
        // A method that sets up the server for connection
        try {
            // Create a ServerSocket and bind it to the specified port
            ServerSocket serverSocket = new ServerSocket(PORT);

            // Get the server's InetAddress
            InetAddress localhost = InetAddress.getLocalHost();

            // Log server information
            System.out.println("Server is listening on the following address:");
            System.out.println("Hostname: " + localhost.getHostName());
            System.out.println("Host Address: " + localhost.getHostAddress());
            System.out.println("Port: " + PORT);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     */
    public void startAcceptingRequests() {
        // A method that starts accepting client requests and handles them in separate threads
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server is listening on port " + PORT);

            // Counter to limit the number of concurrent connections
            int clientCounter = 0;
            int maxClients = 2; // Set the maximum number of concurrent clients

            while (true) {
                if (clientCounter < maxClients) {
                    Socket clientSocket = serverSocket.accept();
                    clientCounter++;

                    // Generate a unique username for the client (you can implement your own logic)
                    String username = "User" + clientCounter;

                    // Create a new ServerHandler instance and start a separate thread for each client
                    ServerHandler handler = new ServerHandler(clientSocket, username);
                    handler.start();

                    System.out.println("Client " + username + " connected.");
                } else {
                    // You can optionally reject additional connections if you have reached the limit
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

    /**
     *
     * @return
     */
    public int getPort() {
        // Getter for the PORT attribute
        return PORT;
    }


    /**
     *
     * @param args
     */
    public static void main(String[] args) {
        // The static main method that instantiates the class, sets up the server, and starts accepting requests
        SocketServer server = new SocketServer();
        server.setup();
        server.startAcceptingRequests();
    }
}

