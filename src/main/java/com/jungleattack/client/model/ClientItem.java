package com.jungleattack.client.model;

import com.jungleattack.client.util.Position;

/** Un café o una bandera. */
public class ClientItem {
    private final int id;
    private final Position position;
    private final String imageFile;

    public ClientItem(int id, Position position, String imageFile) {
        this.id = id;
        this.position = position;
        this.imageFile = imageFile;
    }

    public int getId() { return id; }
    public Position getPosition() { return position; }
    public String getImageFile() { return imageFile; }
}
