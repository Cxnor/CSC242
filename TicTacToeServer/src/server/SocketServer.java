package server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class SocketServer {
    private final int PORT;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
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

    public void startAcceptingRequests() {
        try {
            executorService = Executors.newFixedThreadPool(10);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                ServerHandler serverHandler = new ServerHandler(clientSocket);
                executorService.submit(serverHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Do not close the serverSocket here
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdown();
            }
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

