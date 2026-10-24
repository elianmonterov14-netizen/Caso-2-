package com.jungleattack.client.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MessageTest {

    @Test
    void cadaTipoDeMensajeHaceIdaYVuelta() {
        for (MessageType type : MessageType.values()) {
            Message original = new Message(type)
                    .put("texto", "hola")
                    .put("numero", 7)
                    .put("decimal", 2.5)
                    .put("lista", List.of(1, 2, 3));

            Message copia = Message.fromJson(original.toJson());

            assertEquals(type, copia.getType());
            assertEquals("hola", copia.getString("texto"));
            assertEquals(7, copia.getInt("numero"));
            assertEquals(2.5, copia.getDouble("decimal"), 1e-9);
            assertEquals(List.of(1, 2, 3), copia.getIntList("lista"));
        }
    }

    @Test
    void toJsonEsUnaSolaLinea() {
        String json = new Message(MessageType.MATCH_CREATED).put("code", "A7K2").toJson();
        assertFalse(json.contains("\n"));
    }

    @Test
    void lasRutasLleganComoArreglosNoComoObjetos() {
        Message m = Message.fromJson("{\"type\":\"ZONE_UPDATE\",\"payload\":{\"route\":[[1.0,2.0],[3.0,4.0]]}}");
        assertEquals(2, m.getArray("route").size());
        assertTrue(m.getArray("route").get(0).isJsonArray());
    }
}
