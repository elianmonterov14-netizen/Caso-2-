package com.jungleattack.client.view;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.jungleattack.client.controller.ClientController;
import com.jungleattack.client.model.GameModel;
import com.jungleattack.client.model.Species;
import com.jungleattack.client.protocol.Message;

class SelectionPanelTest {

    @Test
    void laEspecieQueYaTomoElRivalQuedaDeshabilitada() {
        GameModel model = new GameModel();
        ClientController controller = new ClientController(model);
        SelectionPanel panel = new SelectionPanel(model, controller);   // sin mostrar la ventana

        assertTrue(panel.isSpeciesEnabled(Species.JAGUAR));

        controller.handleMessage(Message.fromJson(
                "{\"type\":\"ANIMAL_SELECTION\",\"payload\":{\"available\":[\"SLOTH\",\"HOWLER_MONKEY\","
                + "\"TAPIR\",\"TOUCAN\",\"RED_EYED_FROG\",\"COATI\"],\"mySpecies\":null,"
                + "\"opponentSpecies\":\"JAGUAR\"}}"));

        assertFalse(panel.isSpeciesEnabled(Species.JAGUAR));
        assertTrue(panel.isSpeciesEnabled(Species.TAPIR));
    }
}
