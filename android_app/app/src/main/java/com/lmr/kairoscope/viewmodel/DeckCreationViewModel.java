package com.lmr.kairoscope.viewmodel;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.lmr.kairoscope.data.model.DeckCreationRequest;
import com.lmr.kairoscope.data.model.DeckResponse;
import com.lmr.kairoscope.data.repository.DeckRepository;

public class DeckCreationViewModel extends ViewModel {

    private static final String TAG = "DeckCreationViewModel";
    private final DeckRepository deckRepository;

    // LiveData para almacenar los inputs del usuario
    private final MutableLiveData<String> discipline = new MutableLiveData<>("");
    private final MutableLiveData<String> blockDescription = new MutableLiveData<>("");
    private final MutableLiveData<String> selectedColor = new MutableLiveData<>("#31628D"); // Color primario por defecto

    // LiveData para el estado de carga
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // LiveData para mensajes (éxito/error)
    private final MutableLiveData<String> message = new MutableLiveData<>();

    // Crear un LiveData para indicar navegación exitosa
    private final MutableLiveData<Boolean> shouldNavigateToList = new MutableLiveData<>();

    public LiveData<Boolean> getShouldNavigateToList() { return shouldNavigateToList; }

    // Constructor
    public DeckCreationViewModel(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;

        // Observar el resultado de la creación
        this.deckRepository.getDeckCreationResult().observeForever(result -> {
            isLoading.postValue(false);

            if (result.isSuccess()) {
                message.postValue("¡Baraja creada con éxito! Nombre: " + result.getDeck().getName());
                shouldNavigateToList.postValue(true);
            } else {
                message.postValue("Error: " + result.getMessage());
            }
        });
    }

    // Getters y setters para los LiveData
    public LiveData<String> getDiscipline() { return discipline; }
    public void setDiscipline(String value) { discipline.setValue(value); }

    public LiveData<String> getBlockDescription() { return blockDescription; }
    public void setBlockDescription(String value) { blockDescription.setValue(value); }

    public LiveData<String> getSelectedColor() { return selectedColor; }
    public void setSelectedColor(String value) { selectedColor.setValue(value); }

    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<DeckResponse> getDeckCreationResult() { return deckRepository.getDeckCreationResult(); }

    // Método para limpiar un mensaje una vez mostrado
    public void clearMessage() {
        message.setValue(null);
    }

    // Método para crear la baraja
    public void createDeck() {
        // Validar que los campos no estén vacíos
        if (discipline.getValue() == null || discipline.getValue().isEmpty()) {
            message.setValue("Por favor, ingresa una disciplina");
            return;
        }

        if (blockDescription.getValue() == null || blockDescription.getValue().isEmpty()) {
            message.setValue("Por favor, describe tu bloqueo creativo");
            return;
        }

        if (selectedColor.getValue() == null || selectedColor.getValue().isEmpty()) {
            message.setValue("Por favor, selecciona un color");
            return;
        }

        // Comenzar la carga
        isLoading.setValue(true);

        // Crear la solicitud
        DeckCreationRequest request = new DeckCreationRequest(
                discipline.getValue(),
                blockDescription.getValue(),
                selectedColor.getValue()
        );

        // Enviar la solicitud al repositorio
        deckRepository.createDeck(request);
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
            if (modelClass.isAssignableFrom(DeckCreationViewModel.class)) {
                return (T) new DeckCreationViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}