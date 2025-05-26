package com.lmr.kairoscope.data.model;

/**
 * Response model for GET /api/deck/{id}/ endpoint.
 * Contains deck details with all its cards.
 */
public class DeckDetailResponse {
    private String status;
    private Deck deck; // Usar Deck directamente

    // Constructor vacío
    public DeckDetailResponse() {}

    // Constructor completo
    public DeckDetailResponse(String status, Deck deck) {
        this.status = status;
        this.deck = deck;
    }

    // Getters
    public String getStatus() { return status; }
    public Deck getDeck() { return deck; }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setDeck(Deck deck) { this.deck = deck; }

    // Método de utilidad
    public boolean isSuccess() { return "success".equals(status); }
}