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
 * ViewModel for home screen showing user greeting and latest deck
 */
public class HomeViewModel extends ViewModel {

    private final AuthRepository authRepository;
    private final DeckRepository deckRepository;

    // Estado de carga
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // Mensajes para la UI
    private final MutableLiveData<String> message = new MutableLiveData<>();

    // Última baraja creada
    private final MutableLiveData<Deck> latestDeck = new MutableLiveData<>();

    public HomeViewModel(AuthRepository authRepository, DeckRepository deckRepository) {
        this.authRepository = authRepository;
        this.deckRepository = deckRepository;

        // Observar resultado de la lista de barajas para obtener la última
        this.deckRepository.getDeckListResult().observeForever(result -> {
            isLoading.postValue(false);

            if (result != null && result.isSuccess()) {
                if (result.getDecks() != null && !result.getDecks().isEmpty()) {
                    // Obtener la última baraja (la más reciente)
                    Deck latest = result.getDecks().get(result.getDecks().size() - 1);
                    latestDeck.postValue(latest);
                } else {
                    latestDeck.postValue(null); // No hay barajas
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

    // Método para cargar datos iniciales
    public void loadHomeData() {
        isLoading.setValue(true);
        deckRepository.getDeckList();
    }

    // Método para limpiar mensajes
    public void clearMessage() {
        message.setValue(null);
    }

    // Factory para crear el ViewModel
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