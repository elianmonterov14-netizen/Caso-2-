package com.jungleattack.client.model;

import java.util.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jungleattack.client.protocol.Message;
import com.jungleattack.client.util.ClientConstants;
import com.jungleattack.client.util.Position;

/**
 * Todo lo que el cliente sabe de la partida. Es el sujeto del Observer.
 * Solo se toca desde el hilo de Swing (EDT), por eso no necesita synchronized.
 */
public class GameModel {

    /** Un aviso para mostrarle al jugador (error o resumen). La ventana lo muestra con un JOptionPane. */
    public record Notice(String text, boolean error) { }

    private final List<ModelObserver> observers = new ArrayList<>();
    private ScreenState screen = ScreenState.MENU;
    private String nickname = "";
    private String matchCode;
    private String opponent;
    private List<Species> availableSpecies = new ArrayList<>(List.of(Species.values()));
    private Species mySpecies;
    private Species opponentSpecies;
    private List<double[]> paths = new ArrayList<>();
    private final Map<Integer, ClientAnimal> animals = new LinkedHashMap<>();
    private List<ClientItem> items = new ArrayList<>();
    private int viewedZoneId;
    private List<Integer> myZones = new ArrayList<>();
    private int myDamage, myCups, myBotsKilled, seconds;
    private final List<String> events = new ArrayList<>();
    private final Set<Integer> selectedIds = new LinkedHashSet<>();
    private Notice notice;

    // ---- Observer ----

    public void addObserver(ModelObserver o) {
        observers.add(o);
    }

    private void notifyObservers() {
        for (ModelObserver o : new ArrayList<>(observers)) {
            o.modelChanged();
        }
    }

    // ---- Pantalla, menú y selección ----

    public void setScreen(ScreenState screen) {
        this.screen = screen;
        notifyObservers();
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void setMatchCode(String code) {
        this.matchCode = code;
        notifyObservers();
    }

    public void setOpponent(String opponent) {
        this.opponent = opponent;
        notifyObservers();
    }

    public void setSpeciesSelection(List<Species> available, Species mine, Species opponent) {
        this.availableSpecies = new ArrayList<>(available);
        this.mySpecies = mine;
        this.opponentSpecies = opponent;
        notifyObservers();
    }

    /** Deja un aviso para que la ventana lo muestre. */
    public void showNotice(String text, boolean error) {
        this.notice = new Notice(text, error);
        notifyObservers();
    }

    /** Devuelve el aviso pendiente (o null) y lo borra, para mostrarlo una sola vez. */
    public Notice takeNotice() {
        Notice n = notice;
        notice = null;
        return n;
    }

    // ---- Mensajes del servidor ----

    /** GAME_STARTED: guarda los caminos y la zona inicial. */
    public void startGame(Message m) {
        paths = new ArrayList<>();
        for (JsonElement e : m.getArray("paths")) {
            JsonArray line = e.getAsJsonArray();
            paths.add(new double[] {
                line.get(0).getAsDouble(), line.get(1).getAsDouble(),
                line.get(2).getAsDouble(), line.get(3).getAsDouble()
            });
        }
        mySpecies = Species.valueOf(m.getString("mySpecies"));
        opponent = m.getString("opponent");
        viewedZoneId = m.getInt("myZoneId");
        animals.clear();
        items = new ArrayList<>();
        myZones = new ArrayList<>(List.of(viewedZoneId));
        myDamage = myCups = myBotsKilled = seconds = 0;
        events.clear();
        selectedIds.clear();
        notifyObservers();
    }

    /** ZONE_UPDATE: reemplaza animales e items y quita de la selección a los que ya no están. */
    public void applyZoneUpdate(Message m) {
        List<ClientAnimal> newAnimals = new ArrayList<>();
        for (JsonElement e : m.getArray("animals")) {
            JsonObject o = e.getAsJsonObject();
            List<Position> route = new ArrayList<>();
            if (o.has("route")) {
                for (JsonElement pe : o.getAsJsonArray("route")) {
                    JsonArray xy = pe.getAsJsonArray();           // [x, y], no {"x":..}
                    route.add(new Position(xy.get(0).getAsDouble(), xy.get(1).getAsDouble()));
                }
            }
            newAnimals.add(new ClientAnimal(
                    o.get("id").getAsInt(),
                    Species.valueOf(o.get("species").getAsString()),
                    new Position(o.get("x").getAsDouble(), o.get("y").getAsDouble()),
                    o.get("energy").getAsInt(),
                    o.get("mine").getAsBoolean(),
                    o.get("bot").getAsBoolean(),
                    o.get("speed").getAsDouble(),
                    route));
        }

        List<ClientItem> newItems = new ArrayList<>();
        for (JsonElement e : m.getArray("items")) {
            JsonObject o = e.getAsJsonObject();
            newItems.add(new ClientItem(
                    o.get("id").getAsInt(),
                    new Position(o.get("x").getAsDouble(), o.get("y").getAsDouble()),
                    o.get("image").getAsString()));
        }

        setStats(m.getInt("myDamage"), m.getInt("myCups"), m.getInt("myBotsKilled"), m.getInt("seconds"));
        if (m.has("events")) {
            for (JsonElement e : m.getArray("events")) {
                addEvent(e.getAsString());
            }
        }
        replaceZone(newAnimals, newItems, m.getInt("viewedZoneId"), m.getIntList("myZones"));
    }

    private void setStats(int damage, int cups, int botsKilled, int seconds) {
        this.myDamage = damage;
        this.myCups = cups;
        this.myBotsKilled = botsKilled;
        this.seconds = seconds;
    }

    /** Agrega un evento al panel. Solo se guardan los últimos MAX_EVENTS. */
    private void addEvent(String event) {
        events.add(event);
        while (events.size() > ClientConstants.MAX_EVENTS) {
            events.remove(0);
        }
    }

    /** Reemplaza animales e items y saca de la selección a los que ya no están. */
    private void replaceZone(Collection<ClientAnimal> newAnimals, List<ClientItem> newItems,
                            int zoneId, List<Integer> zonesWithMyAnimals) {
        animals.clear();
        for (ClientAnimal a : newAnimals) {
            animals.put(a.getId(), a);
        }
        items = new ArrayList<>(newItems);
        viewedZoneId = zoneId;
        myZones = new ArrayList<>(zonesWithMyAnimals);
        selectedIds.retainAll(animals.keySet());
        notifyObservers();
    }

    /** Lo llama GamePanel cada 33 ms. No avisa a los observadores: el Timer ya hace repaint(). */
    public void moveAll(double seconds) {
        for (ClientAnimal a : animals.values()) {
            a.moveStep(seconds);
        }
    }

    // ---- Selección (coordenadas del mapa, no de la pantalla) ----

    /** El animal más cercano al punto, dentro de CLICK_RADIUS. */
    public ClientAnimal findAnimalAt(double x, double y) {
        Position click = new Position(x, y);
        ClientAnimal best = null;
        double bestDistance = ClientConstants.CLICK_RADIUS;
        for (ClientAnimal a : animals.values()) {
            double d = a.getPosition().distanceTo(click);
            if (d <= bestDistance) {
                bestDistance = d;
                best = a;
            }
        }
        return best;
    }

    public void selectOnly(int id) {
        selectedIds.clear();
        selectedIds.add(id);
        notifyObservers();
    }

    /** Selecciona todos MIS animales que queden dentro del rectángulo. */
    public void selectInside(double x1, double y1, double x2, double y2) {
        double minX = Math.min(x1, x2);
        double maxX = Math.max(x1, x2);
        double minY = Math.min(y1, y2);
        double maxY = Math.max(y1, y2);
        selectedIds.clear();
        for (ClientAnimal a : animals.values()) {
            double ax = a.getPosition().getX();
            double ay = a.getPosition().getY();
            if (a.isMine() && ax >= minX && ax <= maxX && ay >= minY && ay <= maxY) {
                selectedIds.add(a.getId());
            }
        }
        notifyObservers();
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyObservers();
    }

    // ---- Zona que se está viendo (id = fila * 4 + columna, igual que el servidor) ----

    public double getZoneMinX() {
        return (viewedZoneId % ClientConstants.ZONE_COLUMNS) * ClientConstants.ZONE_WIDTH;
    }

    public double getZoneMinY() {
        return (viewedZoneId / ClientConstants.ZONE_COLUMNS) * ClientConstants.ZONE_HEIGHT;
    }

    /** Para volver al menú. No borra el aviso pendiente. */
    public void reset() {
        screen = ScreenState.MENU;
        matchCode = null;
        opponent = null;
        availableSpecies = new ArrayList<>(List.of(Species.values()));
        mySpecies = null;
        opponentSpecies = null;
        paths = new ArrayList<>();
        animals.clear();
        items = new ArrayList<>();
        viewedZoneId = 0;
        myZones = new ArrayList<>();
        myDamage = myCups = myBotsKilled = seconds = 0;
        events.clear();
        selectedIds.clear();
        notifyObservers();
    }

    // ---- Getters ----

    public ScreenState getScreen() { return screen; }
    public String getNickname() { return nickname; }
    public String getMatchCode() { return matchCode; }
    public String getOpponent() { return opponent; }
    public List<Species> getAvailableSpecies() { return availableSpecies; }
    public Species getMySpecies() { return mySpecies; }
    public Species getOpponentSpecies() { return opponentSpecies; }
    public List<double[]> getPaths() { return paths; }
    public Collection<ClientAnimal> getAnimals() { return animals.values(); }
    public ClientAnimal getAnimal(int id) { return animals.get(id); }
    public List<ClientItem> getItems() { return items; }
    public int getViewedZoneId() { return viewedZoneId; }
    public List<Integer> getMyZones() { return myZones; }
    public int getMyDamage() { return myDamage; }
    public int getMyCups() { return myCups; }
    public int getMyBotsKilled() { return myBotsKilled; }
    public int getSeconds() { return seconds; }
    public List<String> getEvents() { return events; }
    public Set<Integer> getSelectedIds() { return selectedIds; }
}
