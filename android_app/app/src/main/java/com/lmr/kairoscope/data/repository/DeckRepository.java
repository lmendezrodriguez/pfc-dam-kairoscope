package com.lmr.kairoscope.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.lmr.kairoscope.data.model.DeckCreationRequest;
import com.lmr.kairoscope.data.model.DeckListResponse;
import com.lmr.kairoscope.data.model.DeckResponse;
import com.lmr.kairoscope.data.network.ApiService;
import com.lmr.kairoscope.data.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeckRepository {

    private final ApiService apiService;
    private final FirebaseAuth firebaseAuth;

    // LiveData para comunicar resultados al ViewModel
    private final MutableLiveData<DeckResponse> deckCreationResult = new MutableLiveData<>();
    private final MutableLiveData<DeckListResponse> deckListResult = new MutableLiveData<>();

    public DeckRepository() {
        // Obtenemos la instancia de ApiService usando RetrofitClient
        this.apiService = RetrofitClient.getInstance().getApiService();
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public LiveData<DeckResponse> getDeckCreationResult() {
        return deckCreationResult;
    }

    public LiveData<DeckListResponse> getDeckListResult() {
        return deckListResult;
    }


    // Método para crear una baraja
    public void createDeck(DeckCreationRequest request) {
        // Obtener el usuario actual
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            // No hay usuario autenticado
            deckCreationResult.postValue(new DeckResponse("error", "Usuario no autenticado"));
            return;
        }

        // Obtener el ID Token de Firebase
        currentUser.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String idToken = task.getResult().getToken();

                        // Crear un objeto JSON con el token y los datos de la solicitud
                        JsonObject requestBody = new JsonObject();
                        requestBody.addProperty("token", idToken);
                        requestBody.addProperty("discipline", request.getDiscipline());
                        requestBody.addProperty("blockDescription", request.getBlockDescription());
                        requestBody.addProperty("color", request.getColor());

                        // Hacer la llamada a la API
                        Call<DeckResponse> call = apiService.createDeck(requestBody);

                        call.enqueue(new Callback<DeckResponse>() {
                            @Override
                            public void onResponse(Call<DeckResponse> call, Response<DeckResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    deckCreationResult.postValue(response.body());
                                } else {
                                    // Error en la respuesta
                                    DeckResponse errorResponse = new DeckResponse("error",
                                            "Error: " + response.code() + " " + response.message());
                                    deckCreationResult.postValue(errorResponse);
                                }
                            }

                            @Override
                            public void onFailure(Call<DeckResponse> call, Throwable t) {
                                // Error de red o conexión
                                DeckResponse errorResponse = new DeckResponse("error",
                                        "Error de conexión: " + t.getMessage());
                                deckCreationResult.postValue(errorResponse);
                            }
                        });

                    } else {
                        // Error al obtener el token
                        deckCreationResult.postValue(new DeckResponse("error",
                                "Error al obtener token de autenticación"));
                    }
                });
    }

    // NUEVO: Método para obtener lista de barajas
    public void getDeckList() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            deckListResult.postValue(new DeckListResponse("error", null, 0));
            return;
        }

        currentUser.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String idToken = task.getResult().getToken();
                        String authHeader = "Bearer " + idToken;

                        Call<DeckListResponse> call = apiService.getDeckList(authHeader);

                        call.enqueue(new Callback<DeckListResponse>() {
                            @Override
                            public void onResponse(Call<DeckListResponse> call, Response<DeckListResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    deckListResult.postValue(response.body());
                                } else {
                                    DeckListResponse errorResponse = new DeckListResponse("error", null, 0);
                                    deckListResult.postValue(errorResponse);
                                }
                            }

                            @Override
                            public void onFailure(Call<DeckListResponse> call, Throwable t) {
                                DeckListResponse errorResponse = new DeckListResponse("error", null, 0);
                                deckListResult.postValue(errorResponse);
                            }
                        });

                    } else {
                        deckListResult.postValue(new DeckListResponse("error", null, 0));
                    }
                });
    }
}