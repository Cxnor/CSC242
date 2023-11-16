package server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
public class SocketServer {
    private final int PORT;
    private ServerSocket serverSocket;
    /**
     *
     */
    public SocketServer() {
        this.PORT = 5000;
    }

    /**
     *
     * @param port
     */
    public SocketServer(int port) {
        this.PORT = port;
    }

    /**
     *
     */
    public void setup() {
        try {
            serverSocket = new ServerSocket(PORT);

            // Get the server's InetAddress
            InetAddress localhost = InetAddress.getLocalHost();
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
        try {
            Socket socketPlayer1  = serverSocket.accept();
            ServerHandler serverHandlerPlayer1 = new ServerHandler(socketPlayer1, "Username1");
            serverHandlerPlayer1.start();
            Socket socketPlayer2  = serverSocket.accept();
            ServerHandler serverHandlerPlayer2 = new ServerHandler(socketPlayer2, "Username2");
            serverHandlerPlayer2.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
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

