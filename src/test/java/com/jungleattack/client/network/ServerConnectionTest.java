package com.jungleattack.client.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.jungleattack.client.controller.ClientController;
import com.jungleattack.client.model.GameModel;
import com.jungleattack.client.protocol.Message;
import com.jungleattack.client.protocol.MessageType;

class ServerConnectionTest {

    /** Un controlador que solo guarda lo que le llega (sin Swing). */
    private static class RecordingController extends ClientController {
        final List<Message> received = new ArrayList<>();
        boolean disconnected;

        RecordingController() {
            super(new GameModel());
        }

        @Override
        public void onMessage(Message m) {
            received.add(m);
        }

        @Override
        public void onDisconnected() {
            disconnected = true;
        }
    }

    @Test
    void sendEscribeUnaLineaJsonCorrecta() {
        StringWriter out = new StringWriter();
        ServerConnection connection = new ServerConnection(
                new BufferedReader(new StringReader("")), new PrintWriter(out), new RecordingController());

        connection.send(new Message(MessageType.CREATE_MATCH).put("nickname", "Ana"));

        String written = out.toString();
        assertTrue(written.endsWith("\n"));
        assertEquals(1, written.split("\n").length);
        Message back = Message.fromJson(written.trim());
        assertEquals(MessageType.CREATE_MATCH, back.getType());
        assertEquals("Ana", back.getString("nickname"));
    }

    @Test
    void cadaLineaLlegaAlControlador() {
        String lines = "{\"type\":\"MATCH_CREATED\",\"payload\":{\"code\":\"A7K2\"}}\n"
                + "esto no es json\n"
                + "{\"type\":\"ERROR\",\"payload\":{\"message\":\"La partida ya está llena\"}}\n";
        RecordingController controller = new RecordingController();
        ServerConnection connection = new ServerConnection(
                new BufferedReader(new StringReader(lines)), new PrintWriter(new StringWriter()), controller);

        connection.run();

        assertEquals(2, controller.received.size());
        assertEquals(MessageType.MATCH_CREATED, controller.received.get(0).getType());
        assertEquals("A7K2", controller.received.get(0).getString("code"));
        assertEquals(MessageType.ERROR, controller.received.get(1).getType());
        assertTrue(controller.disconnected);
    }
}
