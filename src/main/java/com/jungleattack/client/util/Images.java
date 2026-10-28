package com.jungleattack.client.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.parser.SVGLoader;

/** Carga cada SVG una sola vez al iniciar y lo guarda en un mapa, para no leer el archivo en cada cuadro. */
public final class Images {

    private static final Map<String, BufferedImage> images = new HashMap<>();

    private Images() {
    }

    /** Al iniciar: lee cada archivo de IMAGE_FILES con JSVG. */
    public static void loadAll() {
        SVGLoader loader = new SVGLoader();
        for (String file : ClientConstants.IMAGE_FILES) {
            URL url = Images.class.getResource(ClientConstants.IMAGES_FOLDER + file);
            if (url == null) {
                System.err.println("No se encontró la imagen: " + file);
                continue;
            }
            SVGDocument document = loader.load(url);
            if (document == null) {
                System.err.println("No se pudo leer el SVG: " + file);
                continue;
            }
            int[] size = sizeOf(file);
            BufferedImage image = new BufferedImage(size[0], size[1], BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            document.render(null, g, new ViewBox(0, 0, size[0], size[1]));
            g.dispose();
            images.put(file, image);
        }
    }

    // Devuelve la imagen ya cargada (o null si el archivo no existía/no cargó).
    public static BufferedImage get(String fileName) {
        return images.get(fileName);
    }

    // A qué tamaño en píxeles rasterizar cada SVG, según sea fondo de zona, item o animal.
    private static int[] sizeOf(String file) {
        if (ClientConstants.TERRAIN_IMAGES.contains(file)) {
            return new int[] {ClientConstants.ZONE_WIDTH, ClientConstants.ZONE_HEIGHT};
        }
        if (ClientConstants.ITEM_IMAGES.contains(file)) {
            return new int[] {ClientConstants.ITEM_SIZE, ClientConstants.ITEM_SIZE};
        }
        return new int[] {ClientConstants.ANIMAL_SIZE, ClientConstants.ANIMAL_SIZE};
    }
}
