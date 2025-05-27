package com.lmr.kairoscope.data.network;

import com.google.gson.JsonObject;
import com.lmr.kairoscope.data.model.DeckDeleteResponse;
import com.lmr.kairoscope.data.model.DeckDetailResponse;
import com.lmr.kairoscope.data.model.DeckListResponse;
import com.lmr.kairoscope.data.model.DeckResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    // Endpoint para generar una baraja (POST /api/deck/)
    @POST("api/deck/")
    @Headers("Content-Type: application/json")
    Call<DeckResponse> createDeck(@Body JsonObject requestBody);

    // Endpoint para obtener todas las barajas del usuario (GET /api/deck/)
    @GET("api/deck/")
    Call<DeckListResponse> getDeckList(@Header("Authorization") String authHeader);

    // NUEVO: Endpoint para obtener detalles de una baraja específica
    @GET("api/deck/{deckId}/")
    Call<DeckDetailResponse> getDeckDetail(@Path("deckId") int deckId, @Header("Authorization") String authHeader);

    // Endpoint para eliminar una baraja
    @DELETE("api/deck/{deckId}/")
    Call<DeckDeleteResponse> deleteDeck(@Path("deckId") int deckId, @Header("Authorization") String authHeader);

}