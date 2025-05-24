package com.lmr.kairoscope.data.network;

import com.google.gson.JsonObject;
import com.lmr.kairoscope.data.model.DeckResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface ApiService {

    // Endpoint para generar una baraja (POST /api/decks/)
    @POST("api/deck/")
    @Headers("Content-Type: application/json")
    Call<DeckResponse> createDeck(@Body JsonObject requestBody);

    // Cuando implementes más endpoints, los añadirás aquí
}