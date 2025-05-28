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
 * ViewModel for managing deck list data and UI state.
 * Handles loading state and communication with DeckRepository.
 */
public class DeckListViewModel extends ViewModel {

    private final DeckRepository deckRepository;

    // Estado de carga
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // Mensajes para la UI
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public DeckListViewModel(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;

        // Observar resultado de la lista de barajas
        this.deckRepository.getDeckListResult().observeForever(result -> {
            isLoading.postValue(false);

            if (result != null) {
                if (result.isSuccess()) {
                    message.postValue("Barajas cargadas correctamente");
                } else {
                    message.postValue("Error al cargar las barajas. Comprueba tu conexión");
                }
            }
        });

        this.deckRepository.getDeckDeleteResult().observeForever(result -> {
            isLoading.postValue(false);

            if (result != null && result.isSuccess()) {
                message.postValue(result.getMessage());
                // Recargar lista después de eliminar
                loadDeckList();
            } else {
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

    // Getter para el resultado de delete
    public LiveData<DeckDeleteResponse> getDeckDeleteResult() {
        return deckRepository.getDeckDeleteResult();
    }

    // Método para cargar la lista de barajas
    public void loadDeckList() {
        isLoading.setValue(true);
        deckRepository.getDeckList();
    }

    // Método para limpiar mensajes
    public void clearMessage() {
        message.setValue(null);
    }

    // Método para eliminar una baraja
    public void deleteDeck(int deckId) {
        isLoading.setValue(true);
        deckRepository.deleteDeck(deckId);
    }

    // Factory para crear el ViewModel
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