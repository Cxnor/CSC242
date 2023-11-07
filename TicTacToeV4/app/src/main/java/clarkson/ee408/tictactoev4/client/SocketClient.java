package clarkson.ee408.tictactoev4.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import com.google.gson.Gson;

public class SocketClient {
    private static SocketClient instance = null;

    private Socket socket;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Gson gson;

    public SocketClient() {
        // Private constructor to enforce Singleton pattern.
        socket = null;
        inputStream = null;
        outputStream = null;
        gson = new Gson();
    }

    public static synchronized SocketClient getInstance() {
        if (instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    public void close() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T> T sendRequest(Object request, Class<T> responseClass) {
        // Serialize the request using Gson.
        String serializedRequest = gson.toJson(request);

        try {
            // Send the request to the server.
            outputStream.writeUTF(serializedRequest);

            // Receive and deserialize the response.
            String response = inputStream.readUTF();
            return gson.fromJson(response, responseClass);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
