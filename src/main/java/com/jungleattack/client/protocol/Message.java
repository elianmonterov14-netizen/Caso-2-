package com.jungleattack.client.protocol;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * El "sobre" de todos los mensajes: un tipo y un payload con los datos.
 * Ejemplo: new Message(MessageType.MATCH_CREATED).put("code", "A7K2")
 */
public class Message {

    private static final Gson gson = new Gson();

    private final MessageType type;
    private final JsonObject payload;

    // Crea un mensaje nuevo con el payload vacío, listo para encadenar put(...).
    public Message(MessageType type) {
        this(type, new JsonObject());
    }

    // Constructor interno: arma un mensaje ya con su payload (lo usa fromJson).
    private Message(MessageType type, JsonObject payload) {
        this.type = type;
        this.payload = payload;
    }

    /** Agrega un dato (número, texto, booleano o lista). Devuelve "this" para encadenar. */
    public Message put(String key, Object value) {
        if (value instanceof Number) {
            payload.addProperty(key, (Number) value);
        } else if (value instanceof String) {
            payload.addProperty(key, (String) value);
        } else if (value instanceof Boolean) {
            payload.addProperty(key, (Boolean) value);
        } else {
            // Listas, arreglos u objetos: Gson los convierte solo
            payload.add(key, gson.toJsonTree(value));
        }
        return this;
    }

    // Si el payload trae esa clave (algunos campos son opcionales, como "events").
    public boolean has(String key) {
        return payload.has(key);
    }

    // Lee un dato del payload como número entero.
    public int getInt(String key) {
        return payload.get(key).getAsInt();
    }

    // Lee un dato del payload como número decimal.
    public double getDouble(String key) {
        return payload.get(key).getAsDouble();
    }

    // Lee un dato del payload como texto.
    public String getString(String key) {
        return payload.get(key).getAsString();
    }

    // Lee un dato del payload como arreglo JSON (p. ej. una lista de animales).
    public JsonArray getArray(String key) {
        return payload.get(key).getAsJsonArray();
    }

    // Lee un arreglo JSON de enteros (p. ej. "myZones" o "animalIds").
    public List<Integer> getIntList(String key) {
        List<Integer> list = new ArrayList<>();
        for (JsonElement e : getArray(key)) {
            list.add(e.getAsInt());
        }
        return list;
    }

    // Getter: tipo de mensaje (define qué significan los campos del payload).
    public MessageType getType() {
        return type;
    }

    // Getter: el payload crudo, por si hace falta leer algo que no tiene atajo arriba.
    public JsonObject getPayload() {
        return payload;
    }

    /** El mensaje como JSON en una sola línea. */
    public String toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("type", type.name());
        root.add("payload", payload);
        return gson.toJson(root);
    }

    /** Convierte una línea JSON en un Message. */
    public static Message fromJson(String line) {
        JsonObject root = JsonParser.parseString(line).getAsJsonObject();
        MessageType type = MessageType.valueOf(root.get("type").getAsString());
        JsonObject payload;
        if (root.has("payload")) {
            payload = root.getAsJsonObject("payload");
        } else {
            payload = new JsonObject();
        }
        return new Message(type, payload);
    }
}
