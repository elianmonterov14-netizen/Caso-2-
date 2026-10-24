package com.jungleattack.client.model;

import com.jungleattack.client.util.Position;
import org.junit.jupiter.api.Test;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ClientAnimalTest {
    private ClientAnimal animal(double speed, Position... route) {
        return new ClientAnimal(1, Species.JAGUAR, new Position(0, 0), 10, true, false, speed, List.of(route));
    }

    @Test
    void avanzaAVelocidadConstante() {
        ClientAnimal a = animal(100, new Position(1000, 0));
        a.moveStep(0.5);
        assertEquals(50.0, a.getPosition().getX(), 1e-9);
        a.moveStep(0.5);
        assertEquals(100.0, a.getPosition().getX(), 1e-9);
    }

    @Test
    void sigueLaRutaPorVariosPuntos() {
        ClientAnimal a = animal(100, new Position(30, 0), new Position(30, 100));
        a.moveStep(1);
        assertEquals(30.0, a.getPosition().getX(), 1e-9);
        assertEquals(70.0, a.getPosition().getY(), 1e-9);
        assertEquals(1, a.getRoute().size());
    }

    @Test
    void seDetieneAlFinalDeLaRuta() {
        ClientAnimal a = animal(100, new Position(10, 0));
        a.moveStep(5);
        assertEquals(10.0, a.getPosition().getX(), 1e-9);
        assertTrue(a.getRoute().isEmpty());
    }
}
