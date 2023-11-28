package test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Event;
import model.User;
import server.DatabaseHelper;
import server.SocketServer;
import socket.Request;
import socket.Response;
import socket.PairingResponse;

import java.sql.SQLException;




public class PairingTest {

    public static void main(String[] args) throws Exception {
        Thread mainThread = new Thread(() -> {
            try{
                DatabaseHelper.getInstance().truncateTables();
            }catch (SQLException e){
                throw new RuntimeException(e);
            }
            SocketServer.main(null);
        });
        mainThread.start();
        Thread.sleep(1000);

        Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

        User user1 = new User("user1", "temp_pas1", "username1", false);
        User user2 = new User("user2", "temp_pas2", "username2", false);
        User user3 = new User("user3", "temp_pas3", "username3", false);
        User user4 = new User("user4", "temp_pas4", "username4", false);

        SocketClientHelper user1Client = new SocketClientHelper();
        SocketClientHelper user2Client = new SocketClientHelper();
        SocketClientHelper user3Client = new SocketClientHelper();
        SocketClientHelper user4Client = new SocketClientHelper();

        System.out.println("TEST 1");
        Request loginRequest1 = new Request(Request.RequestType.LOGIN, gson.toJson(user1));
        Response loginResponse1 = user1Client.sendRequest(loginRequest1, Response.class);
        System.out.println(gson.toJson(loginResponse1));

        System.out.println("TEST 2");
        Request registerRequest2 = new Request(Request.RequestType.REGISTER, gson.toJson(user1));
        Response registerResponse2 = user2Client.sendRequest(registerRequest2, Response.class);
        System.out.println(gson.toJson(registerResponse2));

        System.out.println("TEST 3");
        user1.setPassword("wrong");
        Request loginRequestWrongPassword = new Request(Request.RequestType.LOGIN, gson.toJson(user1));
        Response responseWrongPassword = user1Client.sendRequest(loginRequestWrongPassword, Response.class);
        System.out.println(gson.toJson(responseWrongPassword));

        System.out.println("TEST 4");
        user1.setPassword("temp_pas1");
        Request loginRequestCorrectPassword = new Request(Request.RequestType.LOGIN, gson.toJson(user1));
        Response responseCorrectPassword = user1Client.sendRequest(loginRequestCorrectPassword, Response.class);
        System.out.println(gson.toJson(responseCorrectPassword));

        Request registerRequestUser2 = new Request(Request.RequestType.REGISTER, gson.toJson(user2));
        user2Client.sendRequest(registerRequestUser2, Response.class);
        Request registerRequestUser3 = new Request(Request.RequestType.REGISTER, gson.toJson(user3));
        user2Client.sendRequest(registerRequestUser3, Response.class);
        Request registerRequestUser4 = new Request(Request.RequestType.REGISTER, gson.toJson(user4));
        user2Client.sendRequest(registerRequestUser4, Response.class);

        System.out.println("TEST 5");
        Request updatePairingRequestUser1 = new Request(Request.RequestType.UPDATE_PAIRING, null);
        PairingResponse pairingResponseUser1 = user1Client.sendRequest(updatePairingRequestUser1, PairingResponse.class);
        System.out.println(gson.toJson(pairingResponseUser1));

        System.out.println("TEST 6");
        Request updatePairingRequestUser2 = new Request(Request.RequestType.UPDATE_PAIRING, null);
        PairingResponse pairingResponseUser2 = user2Client.sendRequest(updatePairingRequestUser2, PairingResponse.class);
        System.out.println(gson.toJson(pairingResponseUser2));

        Request loginRequestUser2 = new Request(Request.RequestType.LOGIN, gson.toJson(user2));
        user2Client.sendRequest(loginRequestUser2, Response.class);

        System.out.println("TEST 7");
        PairingResponse pairingResponseUser1AfterLogin = user1Client.sendRequest(updatePairingRequestUser1, PairingResponse.class);
        System.out.println(gson.toJson(pairingResponseUser1AfterLogin));

        Request loginRequestUser3 = new Request(Request.RequestType.LOGIN, gson.toJson(user3));
        Request loginRequestUser4 = new Request(Request.RequestType.LOGIN, gson.toJson(user4));
        user3Client.sendRequest(loginRequestUser3, Response.class);
        user4Client.sendRequest(loginRequestUser4, Response.class);

        System.out.println("TEST 8");
        PairingResponse pairingResponseUser2AfterLogin = user2Client.sendRequest(updatePairingRequestUser2, PairingResponse.class);
        System.out.println(gson.toJson(pairingResponseUser2AfterLogin));

        System.out.println("TEST 9");
        user4Client.close();

        PairingResponse pairingResponseUser2AfterLogout = user2Client.sendRequest(updatePairingRequestUser2, PairingResponse.class);
        System.out.println(gson.toJson(pairingResponseUser2AfterLogout));

        System.out.println("TEST 10");
        Request loginRequestUser5 = new Request(Request.RequestType.LOGIN, gson.toJson(user4));
        user4Client = new SocketClientHelper();
        user4Client.sendRequest(loginRequestUser5, Response.class);
        PairingResponse pairingResponseUser2AfterLoginAgain = user2Client.sendRequest(updatePairingRequestUser2, PairingResponse.class);
        System.out.println(gson.toJson(pairingResponseUser2AfterLoginAgain));

        System.out.println("TEST 11");
        Request sendInvitationRequest = new Request(Request.RequestType.SEND_INVITATION, gson.toJson(user2.getUsername()));
        Response sendInvitationResponse = user1Client.sendRequest(sendInvitationRequest, Response.class);
        System.out.println(gson.toJson(sendInvitationResponse));

        System.out.println("TEST 12");
        PairingResponse pairingResponseUser2AfterInvitation = user2Client.sendRequest(updatePairingRequestUser2, PairingResponse.class);
        System.out.println(gson.toJson(pairingResponseUser2AfterInvitation));

        System.out.println("TEST 13");
        Event invitationEvent = pairingResponseUser2AfterInvitation.getInvitation();
        Request declineInvitationRequest = new Request(Request.RequestType.DECLINE_INVITATION, gson.toJson(invitationEvent.getEventId()));
        Response declineInvitationResponse = user2Client.sendRequest(declineInvitationRequest, Response.class);
        System.out.println(gson.toJson(declineInvitationResponse));

        System.out.println("TEST 14");
        PairingResponse pairingResponseUser1AfterDecline = user1Client.sendRequest(updatePairingRequestUser1, PairingResponse.class);
        System.out.println(gson.toJson(pairingResponseUser1AfterDecline));

        System.out.println("TEST 15");
        Event declineInvitationResponseEvent = pairingResponseUser1AfterDecline.getInvitationResponse();
        Request acknowledgeResponseRequest = new Request(Request.RequestType.ACKNOWLEDGE_RESPONSE, gson.toJson(declineInvitationResponseEvent.getEventId()));
        Response acknowledgeResponse = user1Client.sendRequest(acknowledgeResponseRequest, Response.class);
        System.out.println(gson.toJson(acknowledgeResponse));

        System.out.println("TEST 16");
        Request sendInvitationRequestUser3 = new Request(Request.RequestType.SEND_INVITATION, gson.toJson(user3.getUsername()));
        Response sendInvitationResponseUser3 = user1Client.sendRequest(sendInvitationRequestUser3, Response.class);
        System.out.println(gson.toJson(sendInvitationResponseUser3));

        System.out.println("TEST 17");
        PairingResponse pairingResponseUser3AfterInvitation = user3Client.sendRequest(updatePairingRequestUser2, PairingResponse.class);
        System.out.println(gson.toJson(pairingResponseUser3AfterInvitation));

        System.out.println("TEST 18");
        Event invitationEventUser3 = pairingResponseUser3AfterInvitation.getInvitation();
        Request acceptInvitationRequestUser3 = new Request(Request.RequestType.ACCEPT_INVITATION, gson.toJson(invitationEventUser3.getEventId()));
        Response acceptInvitationResponseUser3 = user3Client.sendRequest(acceptInvitationRequestUser3, Response.class);
        System.out.println(gson.toJson(acceptInvitationResponseUser3));

        System.out.println("TEST 19");
        PairingResponse pairingResponseUser1AfterAccept = user1Client.sendRequest(updatePairingRequestUser1, PairingResponse.class);
        System.out.println(gson.toJson(pairingResponseUser1AfterAccept));

        System.out.println("TEST 20");
        Event acceptInvitationResponseEventUser1 = pairingResponseUser1AfterAccept.getInvitationResponse();
        Request acknowledgeResponseRequestUser1 = new Request(Request.RequestType.ACKNOWLEDGE_RESPONSE, gson.toJson(acceptInvitationResponseEventUser1.getEventId()));
        Response acknowledgeResponseUser1 = user1Client.sendRequest(acknowledgeResponseRequestUser1, Response.class);
        System.out.println(gson.toJson(acknowledgeResponseUser1));

        System.out.println("TEST 21");
        PairingResponse pairingResponseUser2AfterGameStart = user2Client.sendRequest(updatePairingRequestUser2, PairingResponse.class);
        System.out.println(gson.toJson(pairingResponseUser2AfterGameStart));

        System.out.println("TEST 22");
        Request abortGameRequest = new Request(Request.RequestType.ABORT_GAME, null);
        Response abortGameResponse = user1Client.sendRequest(abortGameRequest, Response.class);
        System.out.println(gson.toJson(abortGameResponse));

        System.out.println("TEST 23");
        PairingResponse pairingResponseUser2AfterGameAbort = user2Client.sendRequest(updatePairingRequestUser2, PairingResponse.class);
        System.out.println(gson.toJson(pairingResponseUser2AfterGameAbort));





    }
}