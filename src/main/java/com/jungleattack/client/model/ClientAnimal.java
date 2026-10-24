package com.jungleattack.client.model;

import java.util.ArrayList;
import java.util.List;

import com.jungleattack.client.util.Position;

/** Un animal tal como lo conoce el cliente. */
public class ClientAnimal {

    private final int id;
    private final Species species;
    private final Position position;
    private final int energy;
    private final boolean mine;
    private final boolean bot;
    private final double speed;
    private final List<Position> route;

    public ClientAnimal(int id, Species species, Position position, int energy,
                        boolean mine, boolean bot, double speed, List<Position> route) {
        this.id = id;
        this.species = species;
        this.position = new Position(position);
        this.energy = energy;
        this.mine = mine;
        this.bot = bot;
        this.speed = speed;
        this.route = new ArrayList<>();
        for (Position p : route) {
            this.route.add(new Position(p));
        }
    }

    /**
     * El mismo cálculo que Animal.moveStep del servidor:
     * avanza speed * seconds píxeles siguiendo la ruta.
     * Si llega a un punto y le sobra distancia, sigue hacia el siguiente.
     */
    public void moveStep(double seconds) {
        double distance = speed * seconds;
        while (distance > 0 && !route.isEmpty()) {
            Position next = route.get(0);
            double d = position.distanceTo(next);
            if (d <= distance) {
                position.setX(next.getX());
                position.setY(next.getY());
                route.remove(0);
                distance = distance - d;
            } else {
                position.moveTowards(next, distance);
                distance = 0;
            }
        }
    }

    public int getId() { return id; }
    public Species getSpecies() { return species; }
    public Position getPosition() { return position; }
    public int getEnergy() { return energy; }
    public boolean isMine() { return mine; }
    public boolean isBot() { return bot; }
    public double getSpeed() { return speed; }
    public List<Position> getRoute() { return route; }
}
