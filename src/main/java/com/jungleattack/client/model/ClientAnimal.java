package com.jungleattack.client.model;

import java.util.ArrayList;
import java.util.List;

import com.jungleattack.client.util.Position;

/** Un animal tal como lo conoce el cliente. */
// MVC: Modelo (dato de la partida; GamePanel solo lo lee para dibujar).
public class ClientAnimal {

    private final int id;
    private final Species species;
    private final Position position;
    private final int energy;
    private final boolean mine;
    private final boolean bot;
    private final double speed;
    private final List<Position> route;

    // Copia la ruta recibida para que nadie de afuera pueda modificarla luego.
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

    // Getter: id único del animal.
    public int getId() { return id; }
    // Getter: especie (define imagen y nombre a mostrar).
    public Species getSpecies() { return species; }
    // Getter: posición actual en el mapa.
    public Position getPosition() { return position; }
    // Getter: energía actual (muere al llegar a 0).
    public int getEnergy() { return energy; }
    // Getter: si es del jugador local.
    public boolean isMine() { return mine; }
    // Getter: si es un animal bot.
    public boolean isBot() { return bot; }
    // Getter: velocidad en píxeles por segundo.
    public double getSpeed() { return speed; }
    // Getter: ruta pendiente (puntos que le faltan por recorrer).
    public List<Position> getRoute() { return route; }
}
