package com.jungleattack.client.model;

// Patrón Observer: contrato que implementan las vistas para enterarse de cambios en GameModel (MVC: Vista).
public interface ModelObserver {
    // Se llama cada vez que el modelo cambia; cada vista decide qué volver a dibujar.
    void modelChanged();
}
