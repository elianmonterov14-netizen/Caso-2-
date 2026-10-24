package com.jungleattack.client.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;
import javax.swing.Timer;

import com.jungleattack.client.controller.ClientController;
import com.jungleattack.client.controller.MouseController;
import com.jungleattack.client.model.ClientAnimal;
import com.jungleattack.client.model.ClientItem;
import com.jungleattack.client.model.GameModel;
import com.jungleattack.client.model.ModelObserver;
import com.jungleattack.client.model.ScreenState;
import com.jungleattack.client.util.ClientConstants;
import com.jungleattack.client.util.Images;

/**
 * La pantalla de juego: a la izquierda la zona que se ve (800 x 600) y a la derecha
 * el panel de datos con el minimapa. No tiene lógica del juego: solo dibuja el modelo.
 */
public class GamePanel extends JPanel implements ModelObserver {

    private final GameModel model;
    private final MouseController mouse;
    private final Timer timer;
    private long lastFrameTime;

    public GamePanel(GameModel model, ClientController controller) {
        this.model = model;
        this.mouse = new MouseController(model, controller);
        setPreferredSize(new Dimension(ClientConstants.PANEL_WIDTH, ClientConstants.ZONE_HEIGHT));
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        // Cada 33 ms: mueve a los animales y vuelve a dibujar
        timer = new Timer(ClientConstants.FRAME_MS, e -> nextFrame());
        model.addObserver(this);
    }

    private void nextFrame() {
        long now = System.nanoTime();
        double seconds = Math.min((now - lastFrameTime) / ClientConstants.NANOS_PER_SECOND,
                ClientConstants.MAX_FRAME_SECONDS);
        lastFrameTime = now;
        model.moveAll(seconds);
        repaint();
    }

    /** El timer solo corre mientras se está jugando. */
    @Override
    public void modelChanged() {
        if (model.getScreen() == ScreenState.PLAYING) {
            if (!timer.isRunning()) {
                lastFrameTime = System.nanoTime();
                timer.start();
            }
        } else if (timer.isRunning()) {
            timer.stop();
        }
        repaint();
    }

    /** Orden: fondo, caminos, items, animales, rectángulo de selección y panel de datos. */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // La zona se dibuja en coordenadas del mapa, recortada a 800 x 600
        Graphics2D map = (Graphics2D) g2.create(0, 0, ClientConstants.ZONE_WIDTH, ClientConstants.ZONE_HEIGHT);
        BufferedImage grass = Images.get(ClientConstants.BACKGROUND_IMAGE);
        if (grass != null) {
            map.drawImage(grass, 0, 0, null);
        } else {
            map.setColor(ClientConstants.GRASS_COLOR);
            map.fillRect(0, 0, ClientConstants.ZONE_WIDTH, ClientConstants.ZONE_HEIGHT);
        }
        map.translate(-model.getZoneMinX(), -model.getZoneMinY());
        drawPaths(map);
        for (ClientItem item : model.getItems()) {
            drawItem(map, item);
        }
        for (ClientAnimal animal : model.getAnimals()) {
            drawAnimal(map, animal);
        }
        map.dispose();

        // Rectángulo de selección (en coordenadas de pantalla)
        Rectangle drag = mouse.getDragRectangle();
        if (drag != null) {
            g2.setColor(ClientConstants.SELECTION_FILL_COLOR);
            g2.fillRect(drag.x, drag.y, drag.width, drag.height);
            g2.setColor(ClientConstants.SELECTION_COLOR);
            g2.setStroke(new BasicStroke(ClientConstants.DRAG_STROKE));
            g2.drawRect(drag.x, drag.y, drag.width, drag.height);
        }

        drawHud(g2);
        g2.dispose();
    }

    private void drawPaths(Graphics2D g) {
        g.setColor(ClientConstants.PATH_COLOR);
        g.setStroke(new BasicStroke(ClientConstants.PATH_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (double[] line : model.getPaths()) {
            g.draw(new Line2D.Double(line[0], line[1], line[2], line[3]));
        }
        g.setStroke(new BasicStroke(ClientConstants.THIN_STROKE));
    }

    /** Dibuja cualquier item (café o bandera) con su imageFile, sin preguntar qué es. */
    private void drawItem(Graphics2D g, ClientItem item) {
        int size = ClientConstants.ITEM_SIZE;
        int x = (int) Math.round(item.getPosition().getX() - size / 2.0);
        int y = (int) Math.round(item.getPosition().getY() - size / 2.0);
        BufferedImage image = Images.get(item.getImageFile());
        if (image != null) {
            g.drawImage(image, x, y, null);
        } else {
            g.setColor(ClientConstants.ITEM_FALLBACK_COLOR);
            g.fillOval(x, y, size, size);
        }
    }

    /** Imagen, barra de energía, borde de color y círculo si está seleccionado. */
    private void drawAnimal(Graphics2D g, ClientAnimal a) {
        int size = ClientConstants.ANIMAL_SIZE;
        int x = (int) Math.round(a.getPosition().getX() - size / 2.0);
        int y = (int) Math.round(a.getPosition().getY() - size / 2.0);

        if (model.getSelectedIds().contains(a.getId())) {
            int pad = ClientConstants.SELECTION_PADDING;
            g.setColor(ClientConstants.SELECTION_COLOR);
            g.setStroke(new BasicStroke(ClientConstants.SELECTION_STROKE));
            g.drawOval(x - pad, y - pad, size + 2 * pad, size + 2 * pad);
        }

        BufferedImage image = Images.get(a.getSpecies().getImageFile());
        if (image != null) {
            g.drawImage(image, x, y, null);
        } else {
            g.setColor(ClientConstants.ANIMAL_FALLBACK_COLOR);
            g.fillOval(x, y, size, size);
        }

        // Borde de color: bot, mío o enemigo
        Color border = a.isBot() ? ClientConstants.BOT_COLOR
                : a.isMine() ? ClientConstants.MY_COLOR : ClientConstants.ENEMY_COLOR;
        g.setColor(border);
        g.setStroke(new BasicStroke(ClientConstants.BORDER_STROKE));
        g.drawRoundRect(x, y, size, size, ClientConstants.BORDER_ARC, ClientConstants.BORDER_ARC);

        // Barra de energía
        int barX = (int) Math.round(a.getPosition().getX() - ClientConstants.ENERGY_BAR_WIDTH / 2.0);
        int barY = y - ClientConstants.ENERGY_BAR_OFFSET;
        double ratio = Math.max(0.0, Math.min(1.0, a.getEnergy() / (double) ClientConstants.MAX_ENERGY));
        g.setColor(ClientConstants.ENERGY_BACK_COLOR);
        g.fillRect(barX, barY, ClientConstants.ENERGY_BAR_WIDTH, ClientConstants.ENERGY_BAR_HEIGHT);
        g.setColor(ratio > ClientConstants.ENERGY_HIGH_RATIO ? ClientConstants.ENERGY_HIGH_COLOR
                : ratio > ClientConstants.ENERGY_LOW_RATIO ? ClientConstants.ENERGY_MEDIUM_COLOR
                : ClientConstants.ENERGY_LOW_COLOR);
        g.fillRect(barX, barY, (int) Math.round(ClientConstants.ENERGY_BAR_WIDTH * ratio),
                ClientConstants.ENERGY_BAR_HEIGHT);
        g.setStroke(new BasicStroke(ClientConstants.THIN_STROKE));
    }

    private void drawHud(Graphics2D g) {
        int x0 = ClientConstants.ZONE_WIDTH;
        int width = ClientConstants.HUD_WIDTH;
        g.setColor(ClientConstants.HUD_COLOR);
        g.fillRect(x0, 0, width, ClientConstants.ZONE_HEIGHT);

        g.setColor(ClientConstants.TEXT_COLOR);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, ClientConstants.HUD_TITLE_FONT_SIZE));
        int x = x0 + ClientConstants.HUD_MARGIN;
        int y = ClientConstants.HUD_TOP;
        String species = model.getMySpecies() == null ? "-" : model.getMySpecies().getDisplayName();
        g.drawString("Especie: " + species, x, y);

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, ClientConstants.HUD_FONT_SIZE));
        long alive = model.getAnimals().stream().filter(ClientAnimal::isMine).count();
        int seconds = model.getSeconds();
        String[] lines = {
            "Daño del pack: " + model.getMyDamage(),
            "Cafés tomados: " + model.getMyCups(),
            "Bots eliminados: " + model.getMyBotsKilled(),
            "Animales vivos: " + alive,
            "Seleccionados: " + model.getSelectedIds().size(),
            String.format("Tiempo: %d:%02d", seconds / ClientConstants.SECONDS_PER_MINUTE,
                    seconds % ClientConstants.SECONDS_PER_MINUTE)
        };
        for (String line : lines) {
            y += ClientConstants.HUD_LINE_HEIGHT;
            g.drawString(line, x, y);
        }

        y += ClientConstants.HUD_SECTION_GAP;
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, ClientConstants.HUD_FONT_SIZE));
        g.drawString("Eventos", x, y);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, ClientConstants.EVENT_FONT_SIZE));
        FontMetrics metrics = g.getFontMetrics();
        for (String event : model.getEvents()) {
            y += ClientConstants.EVENT_LINE_HEIGHT;
            g.drawString(fit(event, metrics, width - 2 * ClientConstants.HUD_MARGIN), x, y);
        }

        drawMiniMap(g);
    }

    /** Recorta el texto con "..." para que no se salga del panel. */
    private String fit(String text, FontMetrics metrics, int maxWidth) {
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String shorter = text;
        while (shorter.length() > 1 && metrics.stringWidth(shorter + ClientConstants.ELLIPSIS) > maxWidth) {
            shorter = shorter.substring(0, shorter.length() - 1);
        }
        return shorter + ClientConstants.ELLIPSIS;
    }

    /** 4 x 3 cuadritos: color donde tengo animales, gris en las demás y borde grueso en la zona que veo. */
    private void drawMiniMap(Graphics2D g) {
        int cell = ClientConstants.MINIMAP_CELL;
        g.setStroke(new BasicStroke(ClientConstants.THIN_STROKE));
        for (int row = 0; row < ClientConstants.ZONE_ROWS; row++) {
            for (int column = 0; column < ClientConstants.ZONE_COLUMNS; column++) {
                int zoneId = row * ClientConstants.ZONE_COLUMNS + column;
                int x = ClientConstants.MINIMAP_X + column * cell;
                int y = ClientConstants.MINIMAP_Y + row * cell;
                g.setColor(model.getMyZones().contains(zoneId)
                        ? ClientConstants.MY_COLOR : ClientConstants.MINIMAP_EMPTY_COLOR);
                g.fillRect(x, y, cell, cell);
                g.setColor(ClientConstants.HUD_COLOR);
                g.drawRect(x, y, cell, cell);
            }
        }
        int viewed = model.getViewedZoneId();
        int inset = ClientConstants.MINIMAP_VIEW_INSET;
        int vx = ClientConstants.MINIMAP_X + (viewed % ClientConstants.ZONE_COLUMNS) * cell;
        int vy = ClientConstants.MINIMAP_Y + (viewed / ClientConstants.ZONE_COLUMNS) * cell;
        g.setColor(ClientConstants.MINIMAP_VIEW_COLOR);
        g.setStroke(new BasicStroke(ClientConstants.MINIMAP_VIEW_STROKE));
        g.drawRect(vx + inset, vy + inset, cell - 2 * inset, cell - 2 * inset);
        g.setStroke(new BasicStroke(ClientConstants.THIN_STROKE));
    }
}
