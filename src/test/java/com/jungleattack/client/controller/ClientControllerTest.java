package com.jungleattack.client.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.jungleattack.client.TestMessages.animal;
import static com.jungleattack.client.TestMessages.zoneUpdate;

import com.jungleattack.client.model.GameModel;
import com.jungleattack.client.model.ScreenState;
import com.jungleattack.client.model.Species;
import com.jungleattack.client.network.ServerConnection;
import com.jungleattack.client.protocol.Message;
import com.jungleattack.client.protocol.MessageType;

class ClientControllerTest {

    private StringWriter out;
    private GameModel model;
    private ClientController controller;

    @BeforeEach
    void setUp() {
        out = new StringWriter();
        model = new GameModel();
        controller = new ClientController(model);
        controller.setConnection(new ServerConnection(
                new BufferedReader(new StringReader("")), new PrintWriter(out), controller));
    }

    private List<Message> sent() {
        List<Message> list = new ArrayList<>();
        for (String line : out.toString().split("\n")) {
            if (!line.isBlank()) {
                list.add(Message.fromJson(line));
            }
        }
        return list;
    }

    private void putAnimalsAndSelect() {
        model.applyZoneUpdate(zoneUpdate(0, List.of(0), List.of(
                animal(3, Species.JAGUAR, 10, 10, true),
                animal(4, Species.JAGUAR, 20, 20, true))));
        model.selectInside(0, 0, 100, 100);
    }

    @Test
    void moveSelectedToMandaMoveAnimalsConLosIdsCorrectos() {
        putAnimalsAndSelect();

        controller.moveSelectedTo(812, 344);

        List<Message> messages = sent();
        assertEquals(1, messages.size());
        Message m = messages.get(0);
        assertEquals(MessageType.MOVE_ANIMALS, m.getType());
        assertEquals(List.of(3, 4), m.getIntList("animalIds"));
        assertEquals(812.0, m.getDouble("targetX"), 1e-9);
        assertEquals(344.0, m.getDouble("targetY"), 1e-9);
    }

    @Test
    void sinSeleccionNoMandaNada() {
        controller.moveSelectedTo(100, 100);
        controller.attack(9);
        assertTrue(sent().isEmpty());
    }

    @Test
    void attackMandaLosAnimalesYElObjetivo() {
        putAnimalsAndSelect();

        controller.attack(9);

        Message m = sent().get(0);
        assertEquals(MessageType.ATTACK, m.getType());
        assertEquals(List.of(3, 4), m.getIntList("animalIds"));
        assertEquals(9, m.getInt("targetId"));
    }

    @Test
    void viewZoneYSelectSpeciesUsanLosNombresDelProtocolo() {
        controller.viewZone(5);
        controller.selectSpecies(Species.TOUCAN);

        List<Message> messages = sent();
        assertEquals(MessageType.VIEW_ZONE, messages.get(0).getType());
        assertEquals(5, messages.get(0).getInt("zoneId"));
        assertEquals(MessageType.SELECT_ANIMAL, messages.get(1).getType());
        assertEquals("TOUCAN", messages.get(1).getString("species"));
    }

    @Test
    void matchJoinedCambiaLaPantallaASelecting() {
        controller.handleMessage(Message.fromJson(
                "{\"type\":\"MATCH_JOINED\",\"payload\":{\"code\":\"A7K2\",\"opponent\":\"Luis\"}}"));

        assertEquals(ScreenState.SELECTING, model.getScreen());
        assertEquals("Luis", model.getOpponent());
        assertEquals("A7K2", model.getMatchCode());
    }

    @Test
    void gameStartedPasaAPlaying() {
        controller.handleMessage(Message.fromJson(
                "{\"type\":\"GAME_STARTED\",\"payload\":{\"columns\":4,\"rows\":3,\"zoneWidth\":800,"
                + "\"zoneHeight\":600,\"paths\":[[0,0,100,0]],\"myZoneId\":6,\"mySpecies\":\"TAPIR\","
                + "\"opponent\":\"Luis\"}}"));

        assertEquals(ScreenState.PLAYING, model.getScreen());
        assertEquals(6, model.getViewedZoneId());
        assertEquals(1, model.getPaths().size());
        assertEquals(Species.TAPIR, model.getMySpecies());
    }

    @Test
    void errorYGameOverDejanUnAvisoYGameOverVuelveAlMenu() {
        controller.handleMessage(Message.fromJson(
                "{\"type\":\"ERROR\",\"payload\":{\"message\":\"La partida ya está llena\"}}"));
        GameModel.Notice error = model.takeNotice();
        assertNotNull(error);
        assertTrue(error.error());
        assertEquals("La partida ya está llena", error.text());
        assertNull(model.takeNotice());

        model.setScreen(ScreenState.PLAYING);
        controller.handleMessage(Message.fromJson(
                "{\"type\":\"GAME_OVER\",\"payload\":{\"winner\":\"Ana\",\"reason\":\"bandera\",\"seconds\":125,"
                + "\"players\":[{\"nickname\":\"Ana\",\"species\":\"JAGUAR\",\"botsKilled\":3,\"cups\":2,"
                + "\"deadAnimals\":1}],\"bots\":[{\"species\":\"SLOTH\",\"deadAnimals\":4}]}}"));
        assertEquals(ScreenState.MENU, model.getScreen());
        GameModel.Notice summary = model.takeNotice();
        assertNotNull(summary);
        assertTrue(summary.text().contains("Ana"));
        assertTrue(summary.text().contains("2:05"));
    }
}
