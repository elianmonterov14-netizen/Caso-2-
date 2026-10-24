package com.jungleattack.client.model;

import static com.jungleattack.client.TestMessages.animal;
import static com.jungleattack.client.TestMessages.item;
import static com.jungleattack.client.TestMessages.zoneUpdate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.jungleattack.client.protocol.Message;
import com.jungleattack.client.protocol.MessageType;

class GameModelTest {

    @Test
    void losObservadoresRecibenElAviso() {
        GameModel m = new GameModel();
        AtomicInteger calls = new AtomicInteger();
        m.addObserver(calls::incrementAndGet);

        m.setScreen(ScreenState.SELECTING);
        m.applyZoneUpdate(zoneUpdate(0, List.of(0), List.of()));

        assertEquals(2, calls.get());
        assertEquals(ScreenState.SELECTING, m.getScreen());
    }

    @Test
    void applyZoneUpdateReemplazaLosAnimales() {
        GameModel m = new GameModel();
        m.applyZoneUpdate(zoneUpdate(0, List.of(0), List.of(animal(1, Species.JAGUAR, 0, 0, true))));
        m.applyZoneUpdate(zoneUpdate(0, List.of(0), List.of(animal(2, Species.JAGUAR, 0, 0, true))));

        assertNull(m.getAnimal(1));
        assertNotNull(m.getAnimal(2));
    }

    @Test
    void applyZoneUpdateLeeRutasItemsYDatos() {
        GameModel m = new GameModel();
        m.applyZoneUpdate(zoneUpdate(5, List.of(5, 6),
                List.of(animal(1, Species.JAGUAR, 10, 20, true, false,
                        List.of(List.of(30.0, 20.0), List.of(30.0, 50.0)))),
                List.of(item(100, 400, 300, "coffee.svg")),
                List.of("Tu pack tomó un café")));

        assertEquals(5, m.getViewedZoneId());
        assertEquals(800.0, m.getZoneMinX(), 1e-9);     // zona 5 = columna 1, fila 1
        assertEquals(600.0, m.getZoneMinY(), 1e-9);
        assertEquals(List.of(5, 6), m.getMyZones());
        assertEquals(2, m.getAnimal(1).getRoute().size());
        assertEquals("coffee.svg", m.getItems().get(0).getImageFile());
        assertEquals(3, m.getMyDamage());
        assertEquals(1, m.getMyCups());
        assertEquals(2, m.getMyBotsKilled());
        assertEquals(42, m.getSeconds());
        assertEquals(List.of("Tu pack tomó un café"), m.getEvents());
    }

    @Test
    void findAnimalAtEncuentraElAnimalCorrecto() {
        GameModel m = new GameModel();
        m.applyZoneUpdate(zoneUpdate(0, List.of(0), List.of(
                animal(1, Species.JAGUAR, 100, 100, true),
                animal(2, Species.COATI, 110, 100, false))));

        assertEquals(2, m.findAnimalAt(108, 100).getId());
        assertEquals(1, m.findAnimalAt(95, 100).getId());
        assertNull(m.findAnimalAt(500, 500));
    }

    @Test
    void losAnimalesQueYaNoEstanSalenDeLaSeleccion() {
        GameModel m = new GameModel();
        m.applyZoneUpdate(zoneUpdate(0, List.of(0), List.of(
                animal(1, Species.JAGUAR, 0, 0, true), animal(2, Species.JAGUAR, 5, 5, true))));
        m.selectInside(-10, -10, 20, 20);
        assertEquals(Set.of(1, 2), m.getSelectedIds());

        m.applyZoneUpdate(zoneUpdate(0, List.of(0), List.of(animal(2, Species.JAGUAR, 5, 5, true))));

        assertEquals(Set.of(2), m.getSelectedIds());
    }

    @Test
    void selectInsideSoloSeleccionaMisAnimales() {
        GameModel m = new GameModel();
        m.applyZoneUpdate(zoneUpdate(0, List.of(0), List.of(
                animal(1, Species.JAGUAR, 10, 10, true), animal(2, Species.COATI, 12, 12, false))));

        m.selectInside(50, 50, 0, 0);

        assertEquals(Set.of(1), m.getSelectedIds());
    }

    @Test
    void startGameGuardaLosCaminosYLaZonaInicial() {
        GameModel m = new GameModel();
        m.startGame(new Message(MessageType.GAME_STARTED)
                .put("columns", 4).put("rows", 3).put("zoneWidth", 800).put("zoneHeight", 600)
                .put("paths", List.of(List.of(0, 0, 100, 50), List.of(100, 50, 200, 50)))
                .put("myZoneId", 2).put("mySpecies", "COATI").put("opponent", "Luis"));

        assertEquals(2, m.getPaths().size());
        assertEquals(50.0, m.getPaths().get(0)[3], 1e-9);
        assertEquals(2, m.getViewedZoneId());
        assertEquals(Species.COATI, m.getMySpecies());
    }

    @Test
    void soloSeGuardanLosUltimosCincoEventos() {
        GameModel m = new GameModel();
        List<String> events = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            events.add("evento " + i);
        }
        m.applyZoneUpdate(zoneUpdate(0, List.of(0), List.of(), List.of(), events));

        assertEquals(5, m.getEvents().size());
        assertEquals("evento 3", m.getEvents().get(0));
    }
}
