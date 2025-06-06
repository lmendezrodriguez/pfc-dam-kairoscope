package com.lmr.kairoscope.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.lmr.kairoscope.data.model.DeckDeleteResponse;
import com.lmr.kairoscope.data.model.DeckListResponse;
import com.lmr.kairoscope.data.repository.DeckRepository;

/**
 * ViewModel que gestiona la lista de barajas del usuario.
 * Coordina carga, eliminación y validación de límites de barajas.
 */
public class DeckListViewModel extends ViewModel {

    private final DeckRepository deckRepository;

    // Estados de la UI
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    /**
     * Constructor que inicializa el ViewModel con el Repository.
     * Configura observadores para procesar respuestas de lista y eliminación.
     */
    public DeckListViewModel(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;

        // Observar resultado de carga de lista
        this.deckRepository.getDeckListResult().observeForever(result -> {
            isLoading.postValue(false);

            if (result != null && !result.isSuccess()) {
                message.postValue("Error al cargar las barajas. Comprueba tu conexión");
            }
        });

        // Observar resultado de eliminación y recargar lista si es exitosa
        this.deckRepository.getDeckDeleteResult().observeForever(result -> {
            isLoading.postValue(false);

            if (result != null && result.isSuccess()) {
                message.postValue(result.getMessage());
                loadDeckList(); // Recargar lista tras eliminación exitosa
                deckRepository.clearDeleteResult();
            } else if (result != null) {
                message.postValue("Error al eliminar la baraja");
            }
        });
    }

    // Getters para LiveData
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getMessage() {
        return message;
    }

    public LiveData<DeckListResponse> getDeckListResult() {
        return deckRepository.getDeckListResult();
    }

    public LiveData<DeckDeleteResponse> getDeckDeleteResult() {
        return deckRepository.getDeckDeleteResult();
    }

    /**
     * Carga la lista actualizada de barajas desde el servidor.
     */
    public void loadDeckList() {
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
     * Elimina una baraja específica del usuario.
     */
    public void deleteDeck(int deckId) {
        isLoading.setValue(true);
        deckRepository.deleteDeck(deckId);
    }

    /**
     * Verifica si el usuario puede crear una nueva baraja (límite de 8).
     * @return true si puede crear más barajas, false si alcanzó el límite
     */
    public boolean canCreateNewDeck() {
        DeckListResponse currentResponse = getDeckListResult().getValue();
        if (currentResponse != null && currentResponse.isSuccess() &&
                currentResponse.getDecks() != null) {
            return currentResponse.getDecks().size() < 8;
        }
        return true; // Permitir intento si no hay datos cargados
    }

    /**
     * Factory para crear instancias del ViewModel con dependencias.
     */
    public static class Factory extends ViewModelProvider.NewInstanceFactory {
        private final DeckRepository repository;

        public Factory(DeckRepository repository) {
            this.repository = repository;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(DeckListViewModel.class)) {
                return (T) new DeckListViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}