package com.jungleattack.client.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PositionTest {
    @Test
    void distanceTo() {
        assertEquals(5.0, new Position(0, 0).distanceTo(new Position(3, 4)), 1e-9);
    }

    @Test
    void moveTowardsAvanza() {
        Position p = new Position(0, 0);
        p.moveTowards(new Position(10, 0), 4);
        assertEquals(4.0, p.getX(), 1e-9);
        assertEquals(0.0, p.getY(), 1e-9);
    }

    @Test
    void moveTowardsNoSePasaDelDestino() {
        Position p = new Position(0, 0);
        p.moveTowards(new Position(3, 4), 100);
        assertEquals(3.0, p.getX(), 1e-9);
        assertEquals(4.0, p.getY(), 1e-9);
    }
}
