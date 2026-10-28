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
// Patrón Observer: sujeto (guarda y notifica observadores) · MVC: Modelo.
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

    // Patrón Observer: registra una vista para que reciba modelChanged() en cada cambio.
    public void addObserver(ModelObserver o) {
        observers.add(o);
    }

    // Patrón Observer: avisa a todas las vistas registradas que el modelo cambió.
    private void notifyObservers() {
        for (ModelObserver o : new ArrayList<>(observers)) {
            o.modelChanged();
        }
    }

    // ---- Pantalla, menú y selección ----

    // Cambia qué pantalla (menú/selección/juego) debe mostrar MainWindow.
    public void setScreen(ScreenState screen) {
        this.screen = screen;
        notifyObservers();
    }

    // Guarda el nickname elegido (no notifica: no hay vista que dependa solo de esto).
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    // Guarda el código de partida que mandó el servidor.
    public void setMatchCode(String code) {
        this.matchCode = code;
        notifyObservers();
    }

    // Guarda el nombre del rival cuando se une a la partida.
    public void setOpponent(String opponent) {
        this.opponent = opponent;
        notifyObservers();
    }

    // ANIMAL_SELECTION: guarda qué especies quedan disponibles y lo que ya escogió cada jugador.
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

    // Actualiza las estadísticas del jugador que llegan en cada ZONE_UPDATE.
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

    // Deja un solo animal seleccionado (clic simple sobre un animal mío).
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

    // Vacía la selección actual (clic en vacío, o se pierde la selección al cambiar de zona).
    public void clearSelection() {
        selectedIds.clear();
        notifyObservers();
    }

    // ---- Zona que se está viendo (id = fila * 4 + columna, igual que el servidor) ----

    // Coordenada X (en el mapa completo) donde empieza la zona que se ve, para desplazar el dibujo.
    public double getZoneMinX() {
        return (viewedZoneId % ClientConstants.ZONE_COLUMNS) * ClientConstants.ZONE_WIDTH;
    }

    // Coordenada Y (en el mapa completo) donde empieza la zona que se ve, para desplazar el dibujo.
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

    // Getter: pantalla actual (menú, selección o jugando).
    public ScreenState getScreen() { return screen; }
    // Getter: nickname del jugador local.
    public String getNickname() { return nickname; }
    // Getter: código de la partida actual.
    public String getMatchCode() { return matchCode; }
    // Getter: nombre del rival.
    public String getOpponent() { return opponent; }
    // Getter: especies que todavía se pueden escoger.
    public List<Species> getAvailableSpecies() { return availableSpecies; }
    // Getter: especie que escogió el jugador local.
    public Species getMySpecies() { return mySpecies; }
    // Getter: especie que escogió el rival.
    public Species getOpponentSpecies() { return opponentSpecies; }
    // Getter: líneas de todos los caminos del mapa.
    public List<double[]> getPaths() { return paths; }
    // Getter: todos los animales de la zona que se ve.
    public Collection<ClientAnimal> getAnimals() { return animals.values(); }
    // Getter: un animal por id, o null si no está en la zona que se ve.
    public ClientAnimal getAnimal(int id) { return animals.get(id); }
    // Getter: items (cafés/banderas) de la zona que se ve.
    public List<ClientItem> getItems() { return items; }
    // Getter: id de la zona que se está viendo ahora.
    public int getViewedZoneId() { return viewedZoneId; }
    // Getter: ids de las zonas donde el jugador tiene animales vivos (lo que deja ver el minimapa).
    public List<Integer> getMyZones() { return myZones; }
    // Getter: daño acumulado del pack del jugador.
    public int getMyDamage() { return myDamage; }
    // Getter: cafés tomados por el jugador.
    public int getMyCups() { return myCups; }
    // Getter: bots eliminados por el jugador.
    public int getMyBotsKilled() { return myBotsKilled; }
    // Getter: segundos transcurridos de la partida.
    public int getSeconds() { return seconds; }
    // Getter: últimos eventos para el panel de datos.
    public List<String> getEvents() { return events; }
    // Getter: ids de los animales seleccionados actualmente.
    public Set<Integer> getSelectedIds() { return selectedIds; }
}
