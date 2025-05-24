package com.lmr.kairoscope.data.model;

public class Deck {
    /**
     * Clase que representa una baraja en la aplicación.
     */
    private int id;
    private String name;
    private String discipline;
    private String chosen_color;
    private String created_at;
    private int card_count;

    // Constructor vacío (requerido para Gson)
    public Deck() {}

    // Constructor completo
    public Deck(int id, String name, String discipline, String chosen_color,
                String created_at, int card_count) {
        this.id = id;
        this.name = name;
        this.discipline = discipline;
        this.chosen_color = chosen_color;
        this.created_at = created_at;
        this.card_count = card_count;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getDiscipline() { return discipline; }
    public String getChosen_color() { return chosen_color; }
    public String getCreated_at() { return created_at; }
    public int getCard_count() { return card_count; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDiscipline(String discipline) { this.discipline = discipline; }
    public void setChosen_color(String chosen_color) { this.chosen_color = chosen_color; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }
    public void setCard_count(int card_count) { this.card_count = card_count; }
}