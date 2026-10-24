package com.jungleattack.client.util;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/** Todos los números, colores, textos y nombres de archivo del cliente. */
public final class ClientConstants {

    private ClientConstants() {
    }

    // Red
    public static final String DEFAULT_HOST = "localhost";
    public static final int SERVER_PORT = 5000;
    public static final int CONNECT_TIMEOUT_MS = 3000;

    // Ventana
    public static final String WINDOW_TITLE = "Jungle Attack";
    public static final int ZONE_WIDTH = 800;
    public static final int ZONE_HEIGHT = 600;
    public static final int HUD_WIDTH = 220;
    public static final int PANEL_WIDTH = ZONE_WIDTH + HUD_WIDTH;

    // Mapa (igual que el servidor: id de zona = fila * ZONE_COLUMNS + columna)
    public static final int ZONE_COLUMNS = 4;
    public static final int ZONE_ROWS = 3;

    // Dibujo
    public static final int FPS = 30;
    public static final int FRAME_MS = 33;
    public static final int ANIMAL_SIZE = 32;
    public static final int ITEM_SIZE = 24;
    public static final int PATH_WIDTH = 14;
    public static final int MINIMAP_CELL = 40;
    public static final double NANOS_PER_SECOND = 1_000_000_000.0;
    public static final double MAX_FRAME_SECONDS = 0.25;
    public static final int SECONDS_PER_MINUTE = 60;

    // Barra de energía
    public static final int ENERGY_BAR_WIDTH = 30;
    public static final int ENERGY_BAR_HEIGHT = 4;
    public static final int ENERGY_BAR_OFFSET = 8;
    public static final int MAX_ENERGY = 20;
    public static final double ENERGY_HIGH_RATIO = 0.5;
    public static final double ENERGY_LOW_RATIO = 0.25;

    // Mouse
    public static final int CLICK_RADIUS = 18;
    public static final int MIN_DRAG = 5;   // si el mouse se movió menos de eso, cuenta como un clic

    // Eventos
    public static final int MAX_EVENTS = 5;

    // Colores
    public static final Color MY_COLOR = new Color(46, 204, 64);
    public static final Color ENEMY_COLOR = new Color(231, 76, 60);
    public static final Color BOT_COLOR = new Color(149, 165, 166);
    public static final Color SELECTION_COLOR = new Color(255, 221, 0);
    public static final Color PATH_COLOR = new Color(194, 154, 108, 160);
    public static final Color SELECTION_FILL_COLOR = new Color(255, 221, 0, 50);
    public static final Color GRASS_COLOR = new Color(76, 140, 74);
    public static final Color HUD_COLOR = new Color(30, 34, 30);
    public static final Color TEXT_COLOR = Color.WHITE;
    public static final Color MINIMAP_EMPTY_COLOR = Color.GRAY;
    public static final Color MINIMAP_VIEW_COLOR = Color.WHITE;
    public static final Color ITEM_FALLBACK_COLOR = Color.ORANGE;
    public static final Color ANIMAL_FALLBACK_COLOR = Color.GRAY;
    public static final Color ENERGY_BACK_COLOR = Color.DARK_GRAY;
    public static final Color ENERGY_HIGH_COLOR = new Color(46, 204, 64);
    public static final Color ENERGY_MEDIUM_COLOR = Color.ORANGE;
    public static final Color ENERGY_LOW_COLOR = Color.RED;

    // Trazos
    public static final float THIN_STROKE = 1f;
    public static final float BORDER_STROKE = 2f;
    public static final float SELECTION_STROKE = 3f;
    public static final float DRAG_STROKE = 1.5f;
    public static final float MINIMAP_VIEW_STROKE = 4f;
    public static final int MINIMAP_VIEW_INSET = 2;
    public static final int SELECTION_PADDING = 5;
    public static final int BORDER_ARC = 8;

    // Panel de datos (a la derecha)
    public static final int HUD_MARGIN = 12;
    public static final int HUD_TOP = 28;
    public static final int HUD_LINE_HEIGHT = 22;
    public static final int HUD_SECTION_GAP = 30;
    public static final int EVENT_LINE_HEIGHT = 16;
    public static final int HUD_TITLE_FONT_SIZE = 14;
    public static final int HUD_FONT_SIZE = 13;
    public static final int EVENT_FONT_SIZE = 11;
    public static final String ELLIPSIS = "...";

    // Minimapa (4 x 3 cuadritos, abajo del panel de datos y centrado)
    public static final int MINIMAP_BOTTOM_MARGIN = 20;
    public static final int MINIMAP_X = ZONE_WIDTH + (HUD_WIDTH - MINIMAP_CELL * ZONE_COLUMNS) / 2;
    public static final int MINIMAP_Y = ZONE_HEIGHT - MINIMAP_CELL * ZONE_ROWS - MINIMAP_BOTTOM_MARGIN;

    // Menú y selección
    public static final float MENU_TITLE_FONT_SIZE = 36f;
    public static final float SELECTION_TITLE_FONT_SIZE = 28f;
    public static final int HOST_FIELD_COLUMNS = 16;
    public static final int CODE_FIELD_COLUMNS = 8;
    public static final int MENU_GAP = 6;
    public static final int SELECTION_ICON_SIZE = 64;
    public static final int SELECTION_GRID_ROWS = 2;
    public static final int SELECTION_GRID_COLUMNS = 4;
    public static final int SELECTION_GRID_GAP = 12;
    public static final int SELECTION_LAYOUT_GAP = 10;
    public static final int SELECTION_BORDER = 20;

    // Imágenes (SVG, en src/main/resources/images/)
    public static final String IMAGES_FOLDER = "/images/";
    public static final String BACKGROUND_IMAGE = "grass.svg";
    public static final List<String> SPECIES_IMAGES = List.of(
            "jaguar.svg", "sloth.svg", "howler_monkey.svg", "tapir.svg",
            "toucan.svg", "red_eyed_frog.svg", "coati.svg");
    public static final List<String> ITEM_IMAGES = List.of("coffee.svg", "flag_red.svg", "flag_blue.svg");
    /** La lista de todos los archivos SVG. */
    public static final List<String> IMAGE_FILES = joinAll(SPECIES_IMAGES, ITEM_IMAGES, List.of(BACKGROUND_IMAGE));

    // Textos
    public static final String TEXT_CONNECTION_LOST = "Se perdió la conexión con el servidor";
    public static final String TEXT_CANNOT_CONNECT = "No se pudo conectar con el servidor (%s:%d)";
    public static final String TEXT_WAITING = "Código: %s. Esperando al otro jugador...";
    public static final String TEXT_NEED_NICKNAME = "Escribe un nickname.";
    public static final String TEXT_NEED_NICKNAME_AND_CODE = "Escribe tu nickname y el código de la partida.";

    private static List<String> joinAll(List<String> first, List<String> second, List<String> third) {
        List<String> all = new ArrayList<>(first);
        all.addAll(second);
        all.addAll(third);
        return List.copyOf(all);
    }
}
