package com.jungleattack.client.model;

import com.jungleattack.client.util.Position;

/** Un café o una bandera. */
// MVC: Modelo (dato de la partida; GamePanel solo lo lee para dibujar).
public class ClientItem {
    private final int id;
    private final Position position;
    private final String imageFile;

    // Guarda los datos del item tal como los mandó el servidor.
    public ClientItem(int id, Position position, String imageFile) {
        this.id = id;
        this.position = position;
        this.imageFile = imageFile;
    }

    // Getter: id único del item.
    public int getId() { return id; }
    // Getter: posición en el mapa.
    public Position getPosition() { return position; }
    // Getter: nombre del archivo SVG a dibujar.
    public String getImageFile() { return imageFile; }
}
