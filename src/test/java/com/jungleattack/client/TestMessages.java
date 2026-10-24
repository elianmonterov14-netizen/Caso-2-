package com.jungleattack.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jungleattack.client.model.Species;
import com.jungleattack.client.protocol.Message;
import com.jungleattack.client.protocol.MessageType;

/** Arma mensajes del servidor para los tests, con los mismos campos de la sección 3 del spec del servidor. */
public final class TestMessages {

    private TestMessages() {
    }

    /** Un animal de un ZONE_UPDATE. La ruta es una lista de puntos [x, y]. */
    public static Map<String, Object> animal(int id, Species species, double x, double y,
                                             boolean mine, boolean bot, List<List<Double>> route) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("species", species.name());
        data.put("x", x);
        data.put("y", y);
        data.put("energy", 20);
        data.put("mine", mine);
        data.put("bot", bot);
        data.put("speed", 80.0);
        data.put("route", route);
        return data;
    }

    public static Map<String, Object> animal(int id, Species species, double x, double y, boolean mine) {
        return animal(id, species, x, y, mine, false, List.of());
    }

    /** Un café o una bandera de un ZONE_UPDATE. */
    public static Map<String, Object> item(int id, double x, double y, String image) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", id);
        data.put("x", x);
        data.put("y", y);
        data.put("image", image);
        return data;
    }

    public static Message zoneUpdate(int viewedZoneId, List<Integer> myZones, List<Map<String, Object>> animals,
                                     List<Map<String, Object>> items, List<String> events) {
        return new Message(MessageType.ZONE_UPDATE)
                .put("viewedZoneId", viewedZoneId)
                .put("myZones", myZones)
                .put("animals", animals)
                .put("items", items)
                .put("myDamage", 3)
                .put("myCups", 1)
                .put("myBotsKilled", 2)
                .put("seconds", 42)
                .put("events", events);
    }

    public static Message zoneUpdate(int viewedZoneId, List<Integer> myZones, List<Map<String, Object>> animals) {
        return zoneUpdate(viewedZoneId, myZones, animals, List.of(), List.of());
    }
}
