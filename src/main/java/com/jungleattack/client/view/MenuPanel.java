package com.jungleattack.client.view;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import com.jungleattack.client.controller.ClientController;
import com.jungleattack.client.model.GameModel;
import com.jungleattack.client.model.ModelObserver;
import com.jungleattack.client.model.ScreenState;
import com.jungleattack.client.util.ClientConstants;

/** El menú: servidor, nickname y código, más los botones "Crear partida" y "Unirse". */
public class MenuPanel extends JPanel implements ModelObserver {

    private final GameModel model;
    private final ClientController controller;
    private final JTextField hostField = new JTextField(ClientConstants.DEFAULT_HOST, ClientConstants.HOST_FIELD_COLUMNS);
    private final JTextField nicknameField = new JTextField(ClientConstants.HOST_FIELD_COLUMNS);
    private final JTextField codeField = new JTextField(ClientConstants.CODE_FIELD_COLUMNS);
    private final JButton createButton = new JButton("Crear partida");
    private final JButton joinButton = new JButton("Unirse");
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);

    public MenuPanel(GameModel model, ClientController controller) {
        this.model = model;
        this.controller = controller;
        setPreferredSize(new Dimension(ClientConstants.PANEL_WIDTH, ClientConstants.ZONE_HEIGHT));
        buildLayout();

        createButton.addActionListener(e -> create());
        joinButton.addActionListener(e -> join());
        model.addObserver(this);
    }

    private void buildLayout() {
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(ClientConstants.MENU_GAP, ClientConstants.MENU_GAP,
                ClientConstants.MENU_GAP, ClientConstants.MENU_GAP);
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel(ClientConstants.WINDOW_TITLE, SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, ClientConstants.MENU_TITLE_FONT_SIZE));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        add(title, c);

        c.gridwidth = 1;
        addRow(1, "Servidor:", hostField, c);
        addRow(2, "Nickname:", nicknameField, c);
        addRow(3, "Código:", codeField, c);

        c.gridx = 0;
        c.gridy = 4;
        add(createButton, c);
        c.gridx = 1;
        add(joinButton, c);

        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 2;
        add(statusLabel, c);
    }

    private void addRow(int row, String label, JTextField field, GridBagConstraints c) {
        c.gridx = 0;
        c.gridy = row;
        add(new JLabel(label), c);
        c.gridx = 1;
        add(field, c);
    }

    private void create() {
        String nickname = nicknameField.getText().trim();
        if (nickname.isEmpty()) {
            statusLabel.setText(ClientConstants.TEXT_NEED_NICKNAME);
            return;
        }
        controller.connect(hostField.getText());
        controller.createMatch(nickname);
    }

    private void join() {
        String nickname = nicknameField.getText().trim();
        String code = codeField.getText().trim();
        if (nickname.isEmpty() || code.isEmpty()) {
            statusLabel.setText(ClientConstants.TEXT_NEED_NICKNAME_AND_CODE);
            return;
        }
        controller.connect(hostField.getText());
        controller.joinMatch(nickname, code);
    }

    @Override
    public void modelChanged() {
        boolean waiting = model.getMatchCode() != null && model.getScreen() == ScreenState.MENU;
        if (waiting) {
            statusLabel.setText(String.format(ClientConstants.TEXT_WAITING, model.getMatchCode()));
        } else {
            statusLabel.setText(" ");
        }
        createButton.setEnabled(!waiting);
        joinButton.setEnabled(!waiting);
        hostField.setEnabled(!waiting);
        nicknameField.setEnabled(!waiting);
        codeField.setEnabled(!waiting);
    }
}
