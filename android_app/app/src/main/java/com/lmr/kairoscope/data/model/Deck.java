package com.lmr.kairoscope.data.model;

import java.util.List;

public class Deck {
    /**
     * Clase que representa una baraja en la aplicación.
     */
    private int id;
    private String name;
    private String discipline;
    private String block_description; // AÑADIR este campo que faltaba
    private String chosen_color;
    private String created_at;
    private int card_count;
    private List<Card> cards; // AÑADIR lista de cartas

    // Constructor vacío (requerido para Gson)
    public Deck() {}

    // Constructor para listas (sin cartas)
    public Deck(int id, String name, String discipline, String chosen_color,
                String created_at, int card_count) {
        this.id = id;
        this.name = name;
        this.discipline = discipline;
        this.chosen_color = chosen_color;
        this.created_at = created_at;
        this.card_count = card_count;
    }

    // Constructor completo (con cartas)
    public Deck(int id, String name, String discipline, String block_description,
                String chosen_color, String created_at, int card_count, List<Card> cards) {
        this(id, name, discipline, chosen_color, created_at, card_count);
        this.block_description = block_description;
        this.cards = cards;
    }

    // Getters existentes
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDiscipline() { return discipline; }
    public String getChosen_color() { return chosen_color; }
    public String getCreated_at() { return created_at; }
    public int getCard_count() { return card_count; }

    // Getters nuevos
    public String getBlock_description() { return block_description; }
    public List<Card> getCards() { return cards; }

    // Setters existentes
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDiscipline(String discipline) { this.discipline = discipline; }
    public void setChosen_color(String chosen_color) { this.chosen_color = chosen_color; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public void setCard_count(int card_count) { this.card_count = card_count; }

    // Setters nuevos
    public void setBlock_description(String block_description) { this.block_description = block_description; }
    public void setCards(List<Card> cards) { this.cards = cards; }
}