package com.lmr.kairoscope.data.model;

import java.util.List;

/**
 * Response model for GET /api/deck/ endpoint.
 * Contains the list of user's decks and metadata.
 */
public class DeckListResponse {
    private String status;
    private List<Deck> decks;
    private int total;

    // Constructor vacío (requerido para Gson)
    public DeckListResponse() {}

    // Constructor completo
    public DeckListResponse(String status, List<Deck> decks, int total) {
        this.status = status;
        this.decks = decks;
        this.total = total;
    }

    // Getters
    public String getStatus() { return status; }
    public List<Deck> getDecks() { return decks; }
    public int getTotal() { return total; }

    // Setters
    public void setStatus(String status) { this.status = status; }
    public void setDecks(List<Deck> decks) { this.decks = decks; }
    public void setTotal(int total) { this.total = total; }

    // Método de utilidad
    public boolean isSuccess() {
        return "success".equals(status);
    }
}