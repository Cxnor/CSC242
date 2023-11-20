package test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.User;
import server.ServerHandler;
import server.DatabaseHelper;
import server.SocketServer;
import socket.Request;
import socket.Response;
import socket.PairingResponse;

import java.sql.SQLException;

public class PairingTest {

    private static final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    public static void main(String[] args) throws InterruptedException {
        Thread mainThread = new Thread(() -> {
            try {
                DatabaseHelper.getInstance().truncateTables();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            SocketServer.main(null);
        });
        mainThread.start();
        Thread.sleep(1000);

        // Set up users and clients
        User user1 = new User("username1", "password1", "displayname1", true);
        User user2 = new User("username2", "password2", "displayname2", true);
        User user3 = new User("username3", "password3", "displayname3", true);
        User user4 = new User("username4", "password4", "displayname4", true);

        SocketClientHelper scUser1 = new SocketClientHelper();
        SocketClientHelper client2 = new SocketClientHelper();
        SocketClientHelper client3 = new SocketClientHelper();
        SocketClientHelper client4 = new SocketClientHelper();

        Request request = new Request(Request.RequestType.REGISTER, gson.toJson(user1));
        Response response = scUser1.sendRequest(request, Response.class);
        System.out.println(gson.toJson(response));


    }
}