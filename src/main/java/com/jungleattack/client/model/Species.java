package com.jungleattack.client.model;

/**
 * Las 7 especies que se pueden escoger. Todas son animales nativos de Costa Rica.
 * Cada una tiene un nombre para mostrar y el nombre de su imagen.
 */
// MVC: Modelo.
public enum Species {
    JAGUAR("Jaguar", "jaguar.svg"),
    SLOTH("Perezoso", "sloth.svg"),
    HOWLER_MONKEY("Mono congo", "howler_monkey.svg"),
    TAPIR("Danta", "tapir.svg"),
    TOUCAN("Tucán", "toucan.svg"),
    RED_EYED_FROG("Rana calzonuda", "red_eyed_frog.svg"),
    COATI("Pizote", "coati.svg");

    private final String displayName;
    private final String imageFile;

    // Asocia cada especie con su nombre en español y su archivo de imagen.
    Species(String displayName, String imageFile) {
        this.displayName = displayName;
        this.imageFile = imageFile;
    }

    // Getter: nombre en español para mostrar en la UI.
    public String getDisplayName() {
        return displayName;
    }

    // Getter: nombre del archivo SVG en src/main/resources/images/.
    public String getImageFile() {
        return imageFile;
    }
}
