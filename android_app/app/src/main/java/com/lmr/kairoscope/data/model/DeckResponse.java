package com.lmr.kairoscope.data.model;

public class DeckResponse {
    private String status;
    private String message;
    private DeckInfo deck;

    public DeckResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public boolean isSuccess() {
        return "success".equals(status);
    }

    public String getMessage() {
        return message;
    }

    public DeckInfo getDeck() {
        return deck;
    }

    // Clase interna para representar el objeto deck en la respuesta
    public static class DeckInfo {
        private int id;
        private String name;
        private String user;
        private String created_at;

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getUser() {
            return user;
        }

        public String getCreatedAt() {
            return created_at;
        }
    }
}