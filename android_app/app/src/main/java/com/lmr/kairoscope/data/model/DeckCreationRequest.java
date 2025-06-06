package com.lmr.kairoscope.data.model;

/**
 * Modelo de datos para la solicitud de creación de una nueva baraja.
 * Encapsula los parámetros necesarios para generar estrategias personalizadas.
 */
public class DeckCreationRequest {
    private String discipline;
    private String blockDescription;
    private String color;

    public DeckCreationRequest(String discipline, String blockDescription, String color) {
        this.discipline = discipline;
        this.blockDescription = blockDescription;
        this.color = color;
    }

    public String getDiscipline() {
        return discipline;
    }

    public void setDiscipline(String discipline) {
        this.discipline = discipline;
    }

    public String getBlockDescription() {
        return blockDescription;
    }

    public void setBlockDescription(String blockDescription) {
        this.blockDescription = blockDescription;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}