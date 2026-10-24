package com.jungleattack.client.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonElement;
import com.jungleattack.client.model.Species;
import com.jungleattack.client.protocol.Message;
import com.jungleattack.client.protocol.MessageType;
import com.jungleattack.client.util.ClientConstants;
import com.jungleattack.client.util.Position;

/**
 * SERVIDOR FALSO para probar el cliente sin el servidor real. Solo sirve para ver el menu, la seleccion y
 * una partida de juguete (animales que caminan por los caminos, cafes, ataques y bandera).
 * NO valida que el cliente sea compatible con el servidor real: eso se prueba con el servidor de verdad.
 *
 * Como correrlo:  mvn test-compile exec:java@fake-server
 * Codigo de partida para "Unirse": TEST
 */
public class FakeServer {

    private static final int ZW = ClientConstants.ZONE_WIDTH;
    private static final int ZH = ClientConstants.ZONE_HEIGHT;
    private static final int COLS = ClientConstants.ZONE_COLUMNS;
    private static final int ROWS = ClientConstants.ZONE_ROWS;
    private static final int DOOR_MARGIN = 40;
    private static final double ANIMAL_SPEED = 80;
    private static final double PICKUP_RADIUS = 20;
    private static final double FLAG_RADIUS = 30;
    private static final double ENCOUNTER_RADIUS = 40;
    private static final int TICK_MS = 50;
    private static final int SYNC_MS = 1000;
    private static final int ATTACK_COOLDOWN_MS = 1000;
    private static final int MY_ZONE = 5;
    private static final int RIVAL_ZONE = 10;
    private static final String CODE = "TEST";
    private static final String RIVAL_NAME = "Rival";
    private static final Species RIVAL_SPECIES = Species.JAGUAR;

    // ---------------------------------------------------------------- mapa (grafo de caminos)
    static class Node {
        final int id;
        final Position pos;
        final List<Node> neighbors = new ArrayList<>();
        Node(int id, Position pos) { this.id = id; this.pos = pos; }
        void connect(Node o) { neighbors.add(o); o.neighbors.add(this); }
    }

    static final List<Node> NODES = new ArrayList<>();
    static final List<Node> DOORS = new ArrayList<>();
    static final Node[] CENTERS = new Node[COLS * ROWS];

    static Node newNode(double x, double y) {
        Node n = new Node(NODES.size(), new Position(x, y));
        NODES.add(n);
        return n;
    }

    static {
        Node[][] door = new Node[COLS * ROWS][4];        // 0 = este, 1 = oeste, 2 = sur, 3 = norte
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int z = row * COLS + col;
                double x0 = col * ZW, y0 = row * ZH, cx = x0 + ZW / 2.0, cy = y0 + ZH / 2.0;
                CENTERS[z] = newNode(cx, cy);
                if (col < COLS - 1) door[z][0] = newNode(x0 + ZW - DOOR_MARGIN, cy);
                if (col > 0) door[z][1] = newNode(x0 + DOOR_MARGIN, cy);
                if (row < ROWS - 1) door[z][2] = newNode(cx, y0 + ZH - DOOR_MARGIN);
                if (row > 0) door[z][3] = newNode(cx, y0 + DOOR_MARGIN);
                for (Node d : door[z]) {
                    if (d != null) { CENTERS[z].connect(d); DOORS.add(d); }
                }
            }
        }
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int z = row * COLS + col;
                if (col < COLS - 1) door[z][0].connect(door[z + 1][1]);
                if (row < ROWS - 1) door[z][2].connect(door[z + COLS][3]);
            }
        }
    }

    static Node nearest(Position p) {
        Node best = null;
        double bestD = Double.MAX_VALUE;
        for (Node n : NODES) {
            double d = n.pos.distanceTo(p);
            if (d < bestD) { bestD = d; best = n; }
        }
        return best;
    }

    /**
     * El nodo al que van los animales cuando el jugador hace clic en p. Normalmente es el nodo mas cercano, pero
     * desde adentro de una zona el nodo mas cercano siempre es de esa zona. Por eso, si es una puerta y el clic
     * cae pasando la puerta (hacia el borde), el destino es la puerta de la zona vecina. Igual que GameMap.findDestinationNode.
     */
    static Node destination(Position p) {
        Node nearest = nearest(p);
        for (Node other : nearest.neighbors) {
            if (zoneAt(other.pos) == zoneAt(nearest.pos)) continue;     // el centro de la misma zona
            double towardX = other.pos.getX() - nearest.pos.getX();
            double towardY = other.pos.getY() - nearest.pos.getY();
            double clickX = p.getX() - nearest.pos.getX();
            double clickY = p.getY() - nearest.pos.getY();
            if (clickX * towardX + clickY * towardY > 0) return other;
        }
        return nearest;
    }

    /** BFS: la ruta con menos saltos. */
    static List<Position> route(Node start, Node end) {
        Map<Node, Node> from = new HashMap<>();
        Deque<Node> queue = new ArrayDeque<>();
        from.put(start, null);
        queue.add(start);
        while (!queue.isEmpty()) {
            Node n = queue.poll();
            if (n == end) break;
            for (Node nb : n.neighbors) {
                if (!from.containsKey(nb)) { from.put(nb, n); queue.add(nb); }
            }
        }
        LinkedList<Position> path = new LinkedList<>();
        for (Node n = end; n != null; n = from.get(n)) path.addFirst(new Position(n.pos));
        return path;
    }

    static int zoneAt(Position p) {
        int col = Math.max(0, Math.min((int) (p.getX() / ZW), COLS - 1));
        int row = Math.max(0, Math.min((int) (p.getY() / ZH), ROWS - 1));
        return row * COLS + col;
    }

    static List<List<Integer>> pathLines() {
        List<List<Integer>> lines = new ArrayList<>();
        for (Node n : NODES) {
            for (Node nb : n.neighbors) {
                if (nb.id > n.id) {
                    lines.add(List.of((int) n.pos.getX(), (int) n.pos.getY(), (int) nb.pos.getX(), (int) nb.pos.getY()));
                }
            }
        }
        return lines;
    }

    // ---------------------------------------------------------------- servidor
    public static void main(String[] args) throws IOException {
        try (ServerSocket server = new ServerSocket(ClientConstants.SERVER_PORT)) {
            System.out.println("[FakeServer] escuchando en el puerto " + ClientConstants.SERVER_PORT
                    + "  (codigo para 'Unirse': " + CODE + ")");
            while (true) {
                Socket socket = server.accept();
                System.out.println("[FakeServer] cliente conectado: " + socket.getRemoteSocketAddress());
                new Thread(new Session(socket), "fake-client").start();
            }
        }
    }

    static class FakeAnimal {
        final int id;
        final Species species;
        final Position pos;
        int energy;
        final boolean mine, bot;
        final double speed;
        final List<Position> route = new ArrayList<>();
        long lastAttack = -ATTACK_COOLDOWN_MS;

        FakeAnimal(int id, Species species, Position pos, int energy, boolean mine, boolean bot, double speed) {
            this.id = id; this.species = species; this.pos = pos; this.energy = energy;
            this.mine = mine; this.bot = bot; this.speed = speed;
        }

        boolean alive() { return energy > 0; }

        /** El mismo calculo que Animal.moveStep del servidor. */
        void moveStep(double seconds) {
            double distance = speed * seconds;
            while (distance > 0 && !route.isEmpty()) {
                Position next = route.get(0);
                double d = pos.distanceTo(next);
                if (d <= distance) {
                    pos.setX(next.getX()); pos.setY(next.getY());
                    route.remove(0);
                    distance -= d;
                } else {
                    pos.moveTowards(next, distance);
                    distance = 0;
                }
            }
        }
    }

    static class Cup {
        final int id; final Position pos; boolean taken;
        Cup(int id, Position pos) { this.id = id; this.pos = pos; }
    }

    static class Session implements Runnable {
        private final Socket socket;
        private final PrintWriter out;
        private final BufferedReader in;
        private ScheduledExecutorService timer;

        private String nickname = "Jugador";
        private Species mySpecies;
        private boolean playing;
        private final List<FakeAnimal> animals = new ArrayList<>();
        private final List<Cup> cups = new ArrayList<>();
        private final Map<Integer, Integer> attackOrders = new HashMap<>();   // atacante -> objetivo
        private final List<String> events = new ArrayList<>();
        private int viewedZone = MY_ZONE;
        private int botsKilled, cupsTaken, damage = 2, deadMine;
        private long startTime, lastTick, lastSync;
        private boolean changed;
        private final Map<Species, Integer> deadBots = new LinkedHashMap<>();

        Session(Socket socket) throws IOException {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    try {
                        handle(Message.fromJson(line));
                    } catch (RuntimeException e) {
                        System.out.println("[FakeServer] mensaje invalido: " + line + " -> " + e);
                    }
                }
            } catch (IOException e) {
                // el cliente se fue
            } finally {
                System.out.println("[FakeServer] cliente desconectado");
                stopLoop();
                try { socket.close(); } catch (IOException e) { /* nada */ }
            }
        }

        private synchronized void send(Message m) {
            out.print(m.toJson() + "\n");
            out.flush();
        }

        private synchronized void handle(Message m) {
            System.out.println("[FakeServer] <- " + m.toJson());
            switch (m.getType()) {
                case CREATE_MATCH:
                    nickname = m.getString("nickname");
                    send(new Message(MessageType.MATCH_CREATED).put("code", CODE));
                    // "el otro jugador" entra 2 segundos despues, para ver el mensaje de espera
                    Executors.newSingleThreadScheduledExecutor().schedule(this::rivalJoins, 2, TimeUnit.SECONDS);
                    break;
                case JOIN_MATCH:
                    nickname = m.getString("nickname");
                    if (!CODE.equalsIgnoreCase(m.getString("code").trim())) {
                        send(new Message(MessageType.ERROR).put("message", "El codigo de partida no existe"));
                    } else {
                        rivalJoins();
                    }
                    break;
                case SELECT_ANIMAL:
                    selectAnimal(m.getString("species"));
                    break;
                case MOVE_ANIMALS:
                    if (playing) moveAnimals(m);
                    break;
                case ATTACK:
                    if (playing) attack(m);
                    break;
                case VIEW_ZONE:
                    if (playing && myZones().contains(m.getInt("zoneId"))) {
                        viewedZone = m.getInt("zoneId");
                        changed = true;
                    }
                    break;
                default:
                    break;
            }
        }

        private void rivalJoins() {
            send(new Message(MessageType.MATCH_JOINED).put("code", CODE).put("opponent", RIVAL_NAME));
            sendSelection();
        }

        private List<String> available() {
            List<String> list = new ArrayList<>();
            for (Species s : Species.values()) {
                if (s != RIVAL_SPECIES && s != mySpecies) list.add(s.name());
            }
            return list;
        }

        private void sendSelection() {
            send(new Message(MessageType.ANIMAL_SELECTION)
                    .put("available", available())
                    .put("mySpecies", mySpecies == null ? "" : mySpecies.name())
                    .put("opponentSpecies", RIVAL_SPECIES.name()));
        }

        private void selectAnimal(String name) {
            Species s;
            try { s = Species.valueOf(name); } catch (IllegalArgumentException e) { s = null; }
            if (s == null || s == RIVAL_SPECIES) {
                send(new Message(MessageType.ERROR).put("message", "Esa especie ya esta tomada"));
                return;
            }
            mySpecies = s;
            sendSelection();
            startGame();
        }

        // ------------------------------------------------------------ partida de juguete
        private void startGame() {
            Random random = new Random(7);
            int id = 1;
            for (int i = 0; i < 10; i++) {
                animals.add(new FakeAnimal(id++, mySpecies, spawn(MY_ZONE, random), 20, true, false, ANIMAL_SPEED));
            }
            id = 100;
            for (int i = 0; i < 3; i++) {
                animals.add(new FakeAnimal(id++, RIVAL_SPECIES, spawn(RIVAL_ZONE, random), 20, false, false, ANIMAL_SPEED));
            }
            Species[] botSpecies = {Species.COATI, Species.SLOTH, Species.TOUCAN};
            int[] botZones = {6, 9, 4};
            for (int p = 0; p < botSpecies.length; p++) {
                deadBots.put(botSpecies[p], 0);
                for (int i = 0; i < 4; i++) {
                    animals.add(new FakeAnimal(id++, botSpecies[p], spawn(botZones[p], random), 10, false, true, 50));
                }
            }
            List<Node> doors = new ArrayList<>(DOORS);
            Collections.shuffle(doors, random);
            for (int i = 0; i < 10; i++) cups.add(new Cup(200 + i, new Position(doors.get(i).pos)));

            playing = true;
            startTime = lastTick = lastSync = System.currentTimeMillis();
            send(new Message(MessageType.GAME_STARTED)
                    .put("columns", COLS).put("rows", ROWS).put("zoneWidth", ZW).put("zoneHeight", ZH)
                    .put("paths", pathLines())
                    .put("myZoneId", MY_ZONE)
                    .put("mySpecies", mySpecies.name())
                    .put("opponent", RIVAL_NAME));
            sendZoneUpdate();
            timer = Executors.newSingleThreadScheduledExecutor();
            timer.scheduleAtFixedRate(this::tick, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS);
        }

        private Position spawn(int zone, Random random) {
            Position c = CENTERS[zone].pos;
            return new Position(c.getX() + random.nextInt(61) - 30, c.getY() + random.nextInt(61) - 30);
        }

        private void stopLoop() {
            playing = false;
            if (timer != null) timer.shutdownNow();
        }

        private FakeAnimal find(int id) {
            for (FakeAnimal a : animals) if (a.id == id && a.alive()) return a;
            return null;
        }

        private void moveAnimals(Message m) {
            Position target = new Position(m.getDouble("targetX"), m.getDouble("targetY"));
            Node end = destination(target);      // el nodo mas cercano al clic (o la puerta del vecino si el clic cae hacia el borde)
            for (int id : m.getIntList("animalIds")) {
                FakeAnimal a = find(id);
                if (a != null && a.mine) {
                    a.route.clear();
                    a.route.addAll(route(nearest(a.pos), end));
                    attackOrders.remove(id);
                }
            }
            changed = true;
        }

        private void attack(Message m) {
            int targetId = m.getInt("targetId");
            FakeAnimal target = find(targetId);
            if (target == null || target.mine) return;
            for (int id : m.getIntList("animalIds")) {
                FakeAnimal a = find(id);
                if (a != null && a.mine) attackOrders.put(id, targetId);
            }
        }

        private List<Integer> myZones() {
            List<Integer> zones = new ArrayList<>();
            for (FakeAnimal a : animals) {
                if (a.mine && a.alive() && !zones.contains(zoneAt(a.pos))) zones.add(zoneAt(a.pos));
            }
            return zones;
        }

        private synchronized void tick() {
            if (!playing) return;
            long now = System.currentTimeMillis();
            double seconds = (now - lastTick) / 1000.0;
            lastTick = now;

            for (FakeAnimal a : animals) if (a.mine && a.alive()) a.moveStep(seconds);

            // cafes
            for (Cup c : cups) {
                if (c.taken) continue;
                for (FakeAnimal a : animals) {
                    if (a.mine && a.alive() && a.pos.distanceTo(c.pos) <= PICKUP_RADIUS) {
                        c.taken = true; cupsTaken++; damage++; changed = true;
                        events.add("Tu pack tomo un cafe");
                        break;
                    }
                }
            }
            // ataques ordenados (1 por segundo)
            for (Iterator<Map.Entry<Integer, Integer>> it = attackOrders.entrySet().iterator(); it.hasNext();) {
                Map.Entry<Integer, Integer> order = it.next();
                FakeAnimal attacker = find(order.getKey());
                FakeAnimal target = find(order.getValue());
                if (attacker == null || target == null || attacker.pos.distanceTo(target.pos) > ENCOUNTER_RADIUS) {
                    it.remove();
                    continue;
                }
                if (now - attacker.lastAttack >= ATTACK_COOLDOWN_MS) {
                    attacker.lastAttack = now;
                    target.energy = Math.max(0, target.energy - damage);
                    changed = true;
                    if (!target.alive()) {
                        events.add("Eliminaste a un " + target.species.getDisplayName());
                        if (target.bot) {
                            deadBots.merge(target.species, 1, Integer::sum);
                            botsKilled++;
                            if (botsKilled % 2 == 0) {
                                for (FakeAnimal a : animals) if (a.mine && a.alive()) a.energy = Math.min(20, a.energy + 1);
                                events.add("Tu pack gano 1 de energia");
                            }
                        }
                    }
                }
            }
            // bandera rival
            Position rivalFlag = CENTERS[RIVAL_ZONE].pos;
            for (FakeAnimal a : animals) {
                if (a.mine && a.alive() && a.pos.distanceTo(rivalFlag) <= FLAG_RADIUS) {
                    gameOver();
                    return;
                }
            }
            // zona vista: si ya no tengo animales ahi, pasa a otra
            List<Integer> zones = myZones();
            if (!zones.isEmpty() && !zones.contains(viewedZone)) {
                viewedZone = zones.get(0);
                changed = true;
            }
            if (changed || now - lastSync >= SYNC_MS) {
                sendZoneUpdate();
                lastSync = now;
                changed = false;
            }
        }

        private int elapsed() { return (int) ((lastTick - startTime) / 1000); }

        private void sendZoneUpdate() {
            List<Map<String, Object>> list = new ArrayList<>();
            for (FakeAnimal a : animals) {
                if (!a.alive()) continue;
                if (a.mine || zoneAt(a.pos) == viewedZone) {
                    Map<String, Object> d = new HashMap<>();
                    d.put("id", a.id);
                    d.put("species", a.species.name());
                    d.put("x", a.pos.getX());
                    d.put("y", a.pos.getY());
                    d.put("energy", a.energy);
                    d.put("mine", a.mine);
                    d.put("bot", a.bot);
                    d.put("speed", a.speed);
                    List<List<Double>> pts = new ArrayList<>();
                    for (Position p : a.route) pts.add(List.of(p.getX(), p.getY()));
                    d.put("route", pts);
                    list.add(d);
                }
            }
            List<Map<String, Object>> items = new ArrayList<>();
            for (Cup c : cups) {
                if (!c.taken && zoneAt(c.pos) == viewedZone) items.add(item(c.id, c.pos, "coffee.svg"));
            }
            if (zoneAt(CENTERS[MY_ZONE].pos) == viewedZone) items.add(item(300, CENTERS[MY_ZONE].pos, "flag_red.svg"));
            if (zoneAt(CENTERS[RIVAL_ZONE].pos) == viewedZone) items.add(item(301, CENTERS[RIVAL_ZONE].pos, "flag_blue.svg"));

            List<String> drained = new ArrayList<>(events);
            events.clear();
            send(new Message(MessageType.ZONE_UPDATE)
                    .put("viewedZoneId", viewedZone)
                    .put("myZones", myZones())
                    .put("animals", list)
                    .put("items", items)
                    .put("myDamage", damage)
                    .put("myCups", cupsTaken)
                    .put("myBotsKilled", botsKilled)
                    .put("seconds", elapsed())
                    .put("events", drained));
        }

        private Map<String, Object> item(int id, Position p, String image) {
            Map<String, Object> d = new HashMap<>();
            d.put("id", id);
            d.put("x", p.getX());
            d.put("y", p.getY());
            d.put("image", image);
            return d;
        }

        private void gameOver() {
            stopLoop();
            int dead = 0;
            for (FakeAnimal a : animals) if (a.mine && !a.alive()) dead++;
            List<Map<String, Object>> players = new ArrayList<>();
            Map<String, Object> me = new HashMap<>();
            me.put("nickname", nickname); me.put("species", mySpecies.name());
            me.put("botsKilled", botsKilled); me.put("cups", cupsTaken); me.put("deadAnimals", dead);
            players.add(me);
            Map<String, Object> rival = new HashMap<>();
            rival.put("nickname", RIVAL_NAME); rival.put("species", RIVAL_SPECIES.name());
            rival.put("botsKilled", 0); rival.put("cups", 0); rival.put("deadAnimals", 0);
            players.add(rival);
            List<Map<String, Object>> bots = new ArrayList<>();
            for (Map.Entry<Species, Integer> e : deadBots.entrySet()) {
                Map<String, Object> b = new HashMap<>();
                b.put("species", e.getKey().name()); b.put("deadAnimals", e.getValue());
                bots.add(b);
            }
            send(new Message(MessageType.GAME_OVER)
                    .put("winner", nickname)
                    .put("reason", "Toco la bandera del rival")
                    .put("seconds", elapsed())
                    .put("players", players)
                    .put("bots", bots));
            System.out.println("[FakeServer] partida terminada");
        }
    }
}
