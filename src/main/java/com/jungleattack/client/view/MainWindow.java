package com.jungleattack.client.view;

import java.awt.CardLayout;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.jungleattack.client.controller.ClientController;
import com.jungleattack.client.model.GameModel;
import com.jungleattack.client.model.ModelObserver;
import com.jungleattack.client.model.ScreenState;
import com.jungleattack.client.util.ClientConstants;
import com.jungleattack.client.util.Images;

/** La ventana. Muestra el menú, la selección o el juego según la pantalla del modelo. */
public class MainWindow extends JFrame implements ModelObserver {

    private final GameModel model;
    private final CardLayout cards = new CardLayout();
    private final JPanel container = new JPanel(cards);

    public MainWindow(GameModel model, ClientController controller) {
        super(ClientConstants.WINDOW_TITLE);
        this.model = model;

        container.add(new MenuPanel(model, controller), ScreenState.MENU.name());
        container.add(new SelectionPanel(model, controller), ScreenState.SELECTING.name());
        container.add(new GamePanel(model, controller), ScreenState.PLAYING.name());
        setContentPane(container);

        model.addObserver(this);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
        cards.show(container, ScreenState.MENU.name());
    }

    @Override
    public void modelChanged() {
        cards.show(container, model.getScreen().name());
        GameModel.Notice notice = model.takeNotice();
        if (notice != null) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, notice.text(),
                    ClientConstants.WINDOW_TITLE,
                    notice.error() ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE));
        }
    }

    public static void main(String[] args) {
        Images.loadAll();
        SwingUtilities.invokeLater(() -> {
            GameModel model = new GameModel();
            ClientController controller = new ClientController(model);
            new MainWindow(model, controller).setVisible(true);
        });
    }
}
