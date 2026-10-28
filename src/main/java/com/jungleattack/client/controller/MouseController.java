package com.jungleattack.client.controller;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import com.jungleattack.client.model.ClientAnimal;
import com.jungleattack.client.model.GameModel;
import com.jungleattack.client.util.ClientConstants;

/** Convierte los eventos del mouse en acciones del controlador. */
// MVC: Controlador.
public class MouseController extends MouseAdapter {

    private final GameModel model;
    private final ClientController controller;
    private Point dragStart;
    private Point dragEnd;

    public MouseController(GameModel model, ClientController controller) {
        this.model = model;
        this.controller = controller;
    }

    // Clic izquierdo: empieza un posible arrastre de selección. Clic derecho: mueve o ataca.
    @Override
    public void mousePressed(MouseEvent e) {
        // Clic en el minimapa (a la derecha, en el panel de datos)
        if (clickMiniMap(e.getX(), e.getY())) {
            return;
        }
        // Fuera de la zona de juego no pasa nada
        if (e.getX() < 0 || e.getX() >= ClientConstants.ZONE_WIDTH
                || e.getY() < 0 || e.getY() >= ClientConstants.ZONE_HEIGHT) {
            return;
        }

        if (e.getButton() == MouseEvent.BUTTON1) {
            dragStart = e.getPoint();
            dragEnd = e.getPoint();
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            double mapX = e.getX() + model.getZoneMinX();
            double mapY = e.getY() + model.getZoneMinY();
            ClientAnimal target = model.findAnimalAt(mapX, mapY);
            if (target == null) {
                controller.moveSelectedTo(mapX, mapY);
            } else if (!target.isMine()) {
                controller.attack(target.getId());
            }
        }
    }

    // Mientras se arrastra con el botón izquierdo, va moviendo la esquina del rectángulo.
    @Override
    public void mouseDragged(MouseEvent e) {
        if (dragStart != null) {
            dragEnd = clamp(e.getPoint());
        }
    }

    // Al soltar: si casi no se movió el mouse cuenta como clic, si no selecciona lo que quedó dentro del rectángulo.
    @Override
    public void mouseReleased(MouseEvent e) {
        if (dragStart == null || e.getButton() != MouseEvent.BUTTON1) {
            return;
        }
        dragEnd = clamp(e.getPoint());
        double moved = dragStart.distance(dragEnd);
        double minX = model.getZoneMinX();
        double minY = model.getZoneMinY();

        if (moved < ClientConstants.MIN_DRAG) {
            // Se trata como un clic normal
            ClientAnimal clicked = model.findAnimalAt(dragStart.x + minX, dragStart.y + minY);
            if (clicked == null) {
                model.clearSelection();
            } else if (clicked.isMine()) {
                model.selectOnly(clicked.getId());
            }
        } else {
            model.selectInside(dragStart.x + minX, dragStart.y + minY,
                               dragEnd.x + minX, dragEnd.y + minY);
        }
        dragStart = null;
        dragEnd = null;
    }

    /** El rectángulo que GamePanel tiene que dibujar (null si no se está arrastrando). */
    public Rectangle getDragRectangle() {
        if (dragStart == null || dragEnd == null
                || dragStart.distance(dragEnd) < ClientConstants.MIN_DRAG) {
            return null;
        }
        int x = Math.min(dragStart.x, dragEnd.x);
        int y = Math.min(dragStart.y, dragEnd.y);
        return new Rectangle(x, y, Math.abs(dragStart.x - dragEnd.x), Math.abs(dragStart.y - dragEnd.y));
    }

    /** Si el clic cae en el minimapa, calcula qué zona es. Solo vale si ahí tiene animales vivos. */
    private boolean clickMiniMap(int x, int y) {
        int relX = x - ClientConstants.MINIMAP_X;
        int relY = y - ClientConstants.MINIMAP_Y;
        int width = ClientConstants.MINIMAP_CELL * ClientConstants.ZONE_COLUMNS;
        int height = ClientConstants.MINIMAP_CELL * ClientConstants.ZONE_ROWS;
        if (relX < 0 || relX >= width || relY < 0 || relY >= height) {
            return false;
        }
        int column = relX / ClientConstants.MINIMAP_CELL;
        int row = relY / ClientConstants.MINIMAP_CELL;
        int zoneId = row * ClientConstants.ZONE_COLUMNS + column;
        if (model.getMyZones().contains(zoneId)) {
            controller.viewZone(zoneId);
        }
        return true;
    }

    // Recorta un punto para que no se salga del área de juego (0..ZONE_WIDTH, 0..ZONE_HEIGHT).
    private Point clamp(Point p) {
        int x = Math.max(0, Math.min(p.x, ClientConstants.ZONE_WIDTH - 1));
        int y = Math.max(0, Math.min(p.y, ClientConstants.ZONE_HEIGHT - 1));
        return new Point(x, y);
    }
}
