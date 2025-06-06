package com.lmr.kairoscope.data.model;

/**
 * Representa una carta de estrategía oblicua.
 */
public class Card {
    private int id;
    private String text;

    // Constructor vacío
    public Card() {}

    // Constructor completo
    public Card(int id, String text) {
        this.id = id;
        this.text = text;
    }

    // Getters
    public int getId() { return id; }
    public String getText() { return text; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setText(String text) { this.text = text; }
}