package com.lmr.kairoscope.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.lmr.kairoscope.data.model.Deck;
import com.lmr.kairoscope.data.model.DeckListResponse;
import com.lmr.kairoscope.data.model.UserProfile;
import com.lmr.kairoscope.data.repository.AuthRepository;
import com.lmr.kairoscope.data.repository.DeckRepository;

/**
 * ViewModel para la pantalla principal que muestra saludo personalizado y última baraja.
 * Combina datos de autenticación y barajas para crear la experiencia de inicio.
 */
public class HomeViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final DeckRepository deckRepository;

    // Estados de la UI
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    // Datos principales
    private final MutableLiveData<Deck> latestDeck = new MutableLiveData<>();

    /**
     * Constructor que inicializa el ViewModel con ambos repositories.
     * Configura observador para procesar lista de barajas y extraer la más reciente.
     */
    public HomeViewModel(AuthRepository authRepository, DeckRepository deckRepository) {
        this.authRepository = authRepository;
        this.deckRepository = deckRepository;

        // Observar lista de barajas para identificar la más reciente
        this.deckRepository.getDeckListResult().observeForever(result -> {
            isLoading.postValue(false);

            if (result != null && result.isSuccess()) {
                if (result.getDecks() != null && !result.getDecks().isEmpty()) {
                    // Obtener la baraja más reciente (última en la lista ordenada por fecha)
                    Deck latest = result.getDecks().get(0); // Asumiendo orden descendente por fecha
                    latestDeck.postValue(latest);
                } else {
                    latestDeck.postValue(null); // Sin barajas disponibles
                }
            } else {
                message.postValue("Error al cargar las barajas");
                latestDeck.postValue(null);
            }
        });
    }

    // Getters para LiveData
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<Deck> getLatestDeck() { return latestDeck; }
    public LiveData<UserProfile> getCurrentUserProfile() { return authRepository.getCurrentUserProfileLiveData(); }
    public LiveData<DeckListResponse> getDeckListResult() { return deckRepository.getDeckListResult(); }

    /**
     * Carga los datos iniciales necesarios para la pantalla de inicio.
     */
    public void loadHomeData() {
        isLoading.setValue(true);
        deckRepository.getDeckList();
    }

    /**
     * Limpia el mensaje actual para evitar que se muestre nuevamente.
     */
    public void clearMessage() {
        message.setValue(null);
    }

    /**
     * Factory para crear instancias del ViewModel con múltiples dependencias.
     */
    public static class Factory extends ViewModelProvider.NewInstanceFactory {
        private final AuthRepository authRepository;
        private final DeckRepository deckRepository;

        public Factory(AuthRepository authRepository, DeckRepository deckRepository) {
            this.authRepository = authRepository;
            this.deckRepository = deckRepository;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(HomeViewModel.class)) {
                return (T) new HomeViewModel(authRepository, deckRepository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}