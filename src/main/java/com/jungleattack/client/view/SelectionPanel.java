package com.jungleattack.client.view;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.jungleattack.client.controller.ClientController;
import com.jungleattack.client.model.GameModel;
import com.jungleattack.client.model.ModelObserver;
import com.jungleattack.client.model.Species;
import com.jungleattack.client.util.ClientConstants;
import com.jungleattack.client.util.Images;

/** La selección: 7 botones, uno por especie. Las que ya no están disponibles salen deshabilitadas. */
// Patrón Observer: observador concreto (modelChanged) · MVC: Vista.
public class SelectionPanel extends JPanel implements ModelObserver {

    private final GameModel model;
    private final Map<Species, JButton> buttons = new EnumMap<>(Species.class);
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);

    // Arma el título, la grilla de 7 botones (uno por especie) y se registra como observador.
    public SelectionPanel(GameModel model, ClientController controller) {
        this.model = model;
        setPreferredSize(new Dimension(ClientConstants.PANEL_WIDTH, ClientConstants.ZONE_HEIGHT));
        setLayout(new BorderLayout(ClientConstants.SELECTION_LAYOUT_GAP, ClientConstants.SELECTION_LAYOUT_GAP));
        setBorder(new EmptyBorder(ClientConstants.SELECTION_BORDER, ClientConstants.SELECTION_BORDER,
                ClientConstants.SELECTION_BORDER, ClientConstants.SELECTION_BORDER));

        JLabel title = new JLabel("Escoge tu animal", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, ClientConstants.SELECTION_TITLE_FONT_SIZE));
        add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(ClientConstants.SELECTION_GRID_ROWS,
                ClientConstants.SELECTION_GRID_COLUMNS, ClientConstants.SELECTION_GRID_GAP,
                ClientConstants.SELECTION_GRID_GAP));
        for (Species species : Species.values()) {
            JButton button = createButton(species, controller);
            buttons.put(species, button);
            grid.add(button);
        }
        add(grid, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        model.addObserver(this);
        modelChanged();
    }

    // Crea el botón de una especie con su ícono y lo conecta para mandar selectSpecies al hacer clic.
    private JButton createButton(Species species, ClientController controller) {
        JButton button = new JButton(species.getDisplayName());
        BufferedImage image = Images.get(species.getImageFile());
        if (image != null) {
            button.setIcon(new ImageIcon(image.getScaledInstance(ClientConstants.SELECTION_ICON_SIZE,
                    ClientConstants.SELECTION_ICON_SIZE, Image.SCALE_SMOOTH)));
            button.setVerticalTextPosition(SwingConstants.BOTTOM);
            button.setHorizontalTextPosition(SwingConstants.CENTER);
        }
        button.addActionListener(e -> controller.selectSpecies(species));
        return button;
    }

    /** Deshabilita las especies que ya no están disponibles y muestra lo que escogió cada jugador. */
    @Override
    public void modelChanged() {
        for (Map.Entry<Species, JButton> entry : buttons.entrySet()) {
            entry.getValue().setEnabled(model.getAvailableSpecies().contains(entry.getKey()));
        }
        statusLabel.setText(buildStatus());
    }

    // Arma el texto "Tú: ... | Rival: ..." con lo que ya escogió cada jugador.
    private String buildStatus() {
        String rival = model.getOpponent() == null ? "Rival" : model.getOpponent();
        String mine = model.getMySpecies() == null ? "todavía no escoges" : model.getMySpecies().getDisplayName();
        String theirs = model.getOpponentSpecies() == null
                ? "está escogiendo..." : model.getOpponentSpecies().getDisplayName();
        return "Tú: " + mine + "   |   " + rival + ": " + theirs;
    }

    /** Para los tests: si el botón de esa especie está habilitado. */
    public boolean isSpeciesEnabled(Species species) {
        return buttons.get(species).isEnabled();
    }
}
