package com.jungleattack.client.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jungleattack.client.model.GameModel;
import com.jungleattack.client.model.ScreenState;
import com.jungleattack.client.model.Species;
import com.jungleattack.client.network.ServerConnection;
import com.jungleattack.client.protocol.Message;
import com.jungleattack.client.protocol.MessageType;
import com.jungleattack.client.util.ClientConstants;

/** El puente entre la red, el modelo y lo que hace el jugador. */
public class ClientController {

    private final GameModel model;
    private ServerConnection connection;

    public ClientController(GameModel model) {
        this.model = model;
    }

    /** Para los tests del paquete: se le pone una conexión falsa (StringWriter). */
    void setConnection(ServerConnection connection) {
        this.connection = connection;
    }

    // ---- Acciones del jugador ----

    /** Abre la conexión con el servidor. Si no se puede, avisa y no queda conexión. */
    public void connect(String host) {
        closeConnection();
        String address = (host == null || host.isBlank()) ? ClientConstants.DEFAULT_HOST : host.trim();
        try {
            ServerConnection c = new ServerConnection(address, ClientConstants.SERVER_PORT, this);
            connection = c;
            c.start();
        } catch (IOException e) {
            model.showNotice(String.format(ClientConstants.TEXT_CANNOT_CONNECT, address,
                    ClientConstants.SERVER_PORT), true);
        }
    }

    public void createMatch(String nickname) {
        model.setNickname(nickname);
        send(new Message(MessageType.CREATE_MATCH).put("nickname", nickname));
    }

    public void joinMatch(String nickname, String code) {
        model.setNickname(nickname);
        send(new Message(MessageType.JOIN_MATCH).put("nickname", nickname).put("code", code));
    }

    public void selectSpecies(Species species) {
        send(new Message(MessageType.SELECT_ANIMAL).put("species", species.name()));
    }

    /** Si no hay nada seleccionado, no manda nada. (x, y) son coordenadas del mapa. */
    public void moveSelectedTo(double x, double y) {
        if (model.getSelectedIds().isEmpty()) {
            return;
        }
        send(new Message(MessageType.MOVE_ANIMALS)
                .put("animalIds", new ArrayList<>(model.getSelectedIds()))
                .put("targetX", x)
                .put("targetY", y));
    }

    /** Si no hay nada seleccionado, no manda nada. */
    public void attack(int targetId) {
        if (model.getSelectedIds().isEmpty()) {
            return;
        }
        send(new Message(MessageType.ATTACK)
                .put("animalIds", new ArrayList<>(model.getSelectedIds()))
                .put("targetId", targetId));
    }

    public void viewZone(int zoneId) {
        send(new Message(MessageType.VIEW_ZONE).put("zoneId", zoneId));
    }

    private void send(Message message) {
        if (connection != null) {
            connection.send(message);
        }
    }

    private void closeConnection() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    // ---- Lo que llega del servidor ----

    /** Se llama desde el hilo de red: el modelo solo se cambia desde el hilo de Swing. */
    public void onMessage(Message m) {
        SwingUtilities.invokeLater(() -> handleMessage(m));
    }

    public void onDisconnected() {
        SwingUtilities.invokeLater(() -> {
            connection = null;
            model.reset();
            model.showNotice(ClientConstants.TEXT_CONNECTION_LOST, true);
        });
    }

    public void handleMessage(Message m) {
        try {
            switch (m.getType()) {
                case MATCH_CREATED:
                    model.setMatchCode(m.getString("code"));
                    break;
                case MATCH_JOINED:
                    model.setMatchCode(m.getString("code"));
                    model.setOpponent(m.getString("opponent"));
                    model.setScreen(ScreenState.SELECTING);
                    break;
                case ANIMAL_SELECTION:
                    model.setSpeciesSelection(readSpeciesList(m, "available"),
                            readSpecies(m, "mySpecies"), readSpecies(m, "opponentSpecies"));
                    break;
                case GAME_STARTED:
                    model.startGame(m);
                    model.setScreen(ScreenState.PLAYING);
                    break;
                case ZONE_UPDATE:
                    model.applyZoneUpdate(m);
                    break;
                case GAME_OVER:
                    String summary = buildSummary(m);
                    model.reset();
                    model.showNotice(summary, false);
                    break;
                case ERROR:
                    model.showNotice(m.getString("message"), true);
                    break;
                default:
                    break;   // los mensajes de cliente -> servidor no llegan aquí
            }
        } catch (RuntimeException e) {
            System.err.println("No se pudo procesar el mensaje " + m.getType() + ": " + e);
        }
    }

    // ---- Ayudantes para leer mensajes ----

    private List<Species> readSpeciesList(Message m, String key) {
        List<Species> list = new ArrayList<>();
        if (m.has(key)) {
            for (JsonElement e : m.getArray(key)) {
                list.add(Species.valueOf(e.getAsString()));
            }
        }
        return list;
    }

    /** La especie, o null si falta, viene null o viene vacía (todavía no la escogió). */
    private Species readSpecies(Message m, String key) {
        if (!m.has(key)) {
            return null;
        }
        JsonElement e = m.getPayload().get(key);
        if (e.isJsonNull() || e.getAsString().isEmpty()) {
            return null;
        }
        return Species.valueOf(e.getAsString());
    }

    private String speciesName(String name) {
        if (name == null || name.isEmpty()) {
            return "sin especie";
        }
        try {
            return Species.valueOf(name).getDisplayName();
        } catch (IllegalArgumentException e) {
            return name;
        }
    }

    /** El texto del resumen final (para el JOptionPane). */
    String buildSummary(Message m) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ganador: ").append(m.getString("winner")).append('\n');
        sb.append("Motivo: ").append(m.getString("reason")).append('\n');
        int total = m.getInt("seconds");
        sb.append(String.format("Duración: %d:%02d%n", total / ClientConstants.SECONDS_PER_MINUTE, total % ClientConstants.SECONDS_PER_MINUTE));

        sb.append("\nJugadores:\n");
        for (JsonElement e : m.getArray("players")) {
            JsonObject p = e.getAsJsonObject();
            sb.append(" - ").append(p.get("nickname").getAsString())
              .append(" (").append(speciesName(p.get("species").getAsString())).append("): ")
              .append(p.get("botsKilled").getAsInt()).append(" bots eliminados, ")
              .append(p.get("cups").getAsInt()).append(" cafés, ")
              .append(p.get("deadAnimals").getAsInt()).append(" animales muertos\n");
        }

        sb.append("\nBots:\n");
        for (JsonElement e : m.getArray("bots")) {
            JsonObject b = e.getAsJsonObject();
            sb.append(" - ").append(speciesName(b.get("species").getAsString()))
              .append(": ").append(b.get("deadAnimals").getAsInt()).append(" muertos\n");
        }
        return sb.toString();
    }
}
