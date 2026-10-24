package com.jungleattack.client.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.jungleattack.client.TestMessages.animal;
import static com.jungleattack.client.TestMessages.zoneUpdate;

import com.jungleattack.client.model.GameModel;
import com.jungleattack.client.model.Species;
import com.jungleattack.client.network.ServerConnection;
import com.jungleattack.client.protocol.Message;
import com.jungleattack.client.protocol.MessageType;
import com.jungleattack.client.util.ClientConstants;

class MouseControllerTest {

    private StringWriter out;
    private GameModel model;
    private MouseController mouse;
    private final JPanel source = new JPanel();

    @BeforeEach
    void setUp() {
        out = new StringWriter();
        model = new GameModel();
        ClientController controller = new ClientController(model);
        controller.setConnection(new ServerConnection(
                new BufferedReader(new StringReader("")), new PrintWriter(out), controller));
        mouse = new MouseController(model, controller);
        model.applyZoneUpdate(zoneUpdate(0, List.of(0), List.of(
                animal(1, Species.JAGUAR, 100, 100, true),
                animal(2, Species.COATI, 300, 300, false, true, List.of()))));
    }

    private MouseEvent event(int id, int x, int y, int button) {
        return new MouseEvent(source, id, 0L, 0, x, y, 1, false, button);
    }

    private void click(int x, int y, int button) {
        mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED, x, y, button));
        mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED, x, y, button));
    }

    private int sentLines() {
        return out.toString().isBlank() ? 0 : out.toString().split("\n").length;
    }

    @Test
    void clicIzquierdoSeleccionaMiAnimalYEnUnLugarVacioQuitaLaSeleccion() {
        click(102, 101, MouseEvent.BUTTON1);
        assertEquals(Set.of(1), model.getSelectedIds());

        click(600, 500, MouseEvent.BUTTON1);
        assertTrue(model.getSelectedIds().isEmpty());
    }

    @Test
    void arrastrarSeleccionaLosAnimalesDentroDelRectangulo() {
        mouse.mousePressed(event(MouseEvent.MOUSE_PRESSED, 50, 50, MouseEvent.BUTTON1));
        mouse.mouseDragged(event(MouseEvent.MOUSE_DRAGGED, 150, 150, MouseEvent.NOBUTTON));
        assertEquals(100, mouse.getDragRectangle().width);
        mouse.mouseReleased(event(MouseEvent.MOUSE_RELEASED, 150, 150, MouseEvent.BUTTON1));

        assertEquals(Set.of(1), model.getSelectedIds());
        assertNull(mouse.getDragRectangle());
    }

    @Test
    void clicDerechoEnLugarVacioMueveYSobreUnEnemigoAtaca() {
        model.selectOnly(1);

        click(500, 400, MouseEvent.BUTTON3);
        click(301, 300, MouseEvent.BUTTON3);

        String[] lines = out.toString().split("\n");
        assertEquals(2, lines.length);
        Message move = Message.fromJson(lines[0]);
        assertEquals(MessageType.MOVE_ANIMALS, move.getType());
        assertEquals(500.0, move.getDouble("targetX"), 1e-9);
        Message attack = Message.fromJson(lines[1]);
        assertEquals(MessageType.ATTACK, attack.getType());
        assertEquals(2, attack.getInt("targetId"));
    }

    @Test
    void elMinimapaSoloCambiaDeZonaDondeTengoAnimales() {
        int cell = ClientConstants.MINIMAP_CELL;
        // zona 2 (columna 2, fila 0): no tengo animales ahí
        click(ClientConstants.MINIMAP_X + 2 * cell + 5, ClientConstants.MINIMAP_Y + 5, MouseEvent.BUTTON1);
        assertEquals(0, sentLines());

        // zona 0: sí
        click(ClientConstants.MINIMAP_X + 5, ClientConstants.MINIMAP_Y + 5, MouseEvent.BUTTON1);
        Message m = Message.fromJson(out.toString().trim());
        assertEquals(MessageType.VIEW_ZONE, m.getType());
        assertEquals(0, m.getInt("zoneId"));
    }
}
