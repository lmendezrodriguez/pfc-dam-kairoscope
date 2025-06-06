package com.lmr.kairoscope.data.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.JsonObject;
import com.lmr.kairoscope.data.model.DeckCreationRequest;
import com.lmr.kairoscope.data.model.DeckDetailResponse;
import com.lmr.kairoscope.data.model.DeckListResponse;
import com.lmr.kairoscope.data.model.DeckResponse;
import com.lmr.kairoscope.data.model.DeckDeleteResponse;
import com.lmr.kairoscope.data.network.ApiService;
import com.lmr.kairoscope.data.network.RetrofitClient;
import com.lmr.kairoscope.util.NetworkUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repositorio responsable de gestionar las operaciones relacionadas con las barajas.
 */
public class DeckRepository {

    private final ApiService apiService;
    private final FirebaseAuth firebaseAuth;
    private final Context context;

    // LiveData para comunicar resultados al ViewModel
    private final MutableLiveData<DeckResponse> deckCreationResult = new MutableLiveData<>();
    private final MutableLiveData<DeckListResponse> deckListResult = new MutableLiveData<>();
    private final MutableLiveData<DeckDetailResponse> deckDetailResult = new MutableLiveData<>();
    private final MutableLiveData<DeckDeleteResponse> deckDeleteResult = new MutableLiveData<>();

    public DeckRepository(Context context) {
        this.context = context;
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

    public LiveData<DeckDetailResponse> getDeckDetailResult() {
        return deckDetailResult;
    }

    public LiveData<DeckDeleteResponse> getDeckDeleteResult() {
        return deckDeleteResult;
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
        // AÑADIR verificación de conexión
        if (!NetworkUtils.isNetworkAvailable(context)) {
            deckCreationResult.postValue(new DeckResponse("error", "Sin conexión a internet"));
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

    // Método para obtener lista de barajas
    public void getDeckList() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            deckListResult.postValue(new DeckListResponse("error", null, 0));
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(context)) {
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

    // Método para obtener detalles de una baraja
    public void getDeckDetail(int deckId) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            deckDetailResult.postValue(new DeckDetailResponse("error", null));
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(context)) {
            // Para getDeckDetail:
            deckDetailResult.postValue(new DeckDetailResponse("error", null));
            // Para deleteDeck:
            deckDeleteResult.postValue(new DeckDeleteResponse("error", "Sin conexión a internet"));
            return;
        }

        currentUser.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String idToken = task.getResult().getToken();
                        String authHeader = "Bearer " + idToken;

                        Call<DeckDetailResponse> call = apiService.getDeckDetail(deckId, authHeader);

                        call.enqueue(new Callback<DeckDetailResponse>() {
                            @Override
                            public void onResponse(Call<DeckDetailResponse> call, Response<DeckDetailResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    deckDetailResult.postValue(response.body());
                                } else {
                                    DeckDetailResponse errorResponse = new DeckDetailResponse("error", null);
                                    deckDetailResult.postValue(errorResponse);
                                }
                            }

                            @Override
                            public void onFailure(Call<DeckDetailResponse> call, Throwable t) {
                                DeckDetailResponse errorResponse = new DeckDetailResponse("error", null);
                                deckDetailResult.postValue(errorResponse);
                            }
                        });

                    } else {
                        deckDetailResult.postValue(new DeckDetailResponse("error", null));
                    }
                });
    }

    // Método para eliminar una baraja
    public void deleteDeck(int deckId) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            deckDeleteResult.postValue(new DeckDeleteResponse("error", "Usuario no autenticado"));
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(context)) {
            // Para getDeckDetail:
            deckDetailResult.postValue(new DeckDetailResponse("error", null));
            // Para deleteDeck:
            deckDeleteResult.postValue(new DeckDeleteResponse("error", "Sin conexión a internet"));
            return;
        }

        currentUser.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String idToken = task.getResult().getToken();
                        String authHeader = "Bearer " + idToken;

                        Call<DeckDeleteResponse> call = apiService.deleteDeck(deckId, authHeader);

                        call.enqueue(new Callback<DeckDeleteResponse>() {
                            @Override
                            public void onResponse(Call<DeckDeleteResponse> call, Response<DeckDeleteResponse> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    deckDeleteResult.postValue(response.body());
                                } else {
                                    DeckDeleteResponse errorResponse = new DeckDeleteResponse("error", "Error al eliminar baraja");
                                    deckDeleteResult.postValue(errorResponse);
                                }
                            }

                            @Override
                            public void onFailure(Call<DeckDeleteResponse> call, Throwable t) {
                                DeckDeleteResponse errorResponse = new DeckDeleteResponse("error", "Error de conexión");
                                deckDeleteResult.postValue(errorResponse);
                            }
                        });

                    } else {
                        deckDeleteResult.postValue(new DeckDeleteResponse("error", "Error de autenticación"));
                    }
                });
    }

    public void clearDeleteResult() {
        deckDeleteResult.postValue(null);
    }
}