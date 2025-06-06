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

/**
 * ViewModel que gestiona la creación de nuevas barajas de estrategias.
 * Almacena los parámetros del usuario y coordina con el Repository.
 */
public class DeckCreationViewModel extends ViewModel {

    private static final String TAG = "DeckCreationViewModel";
    private final DeckRepository deckRepository;

    // Parámetros de entrada del usuario
    private final MutableLiveData<String> discipline = new MutableLiveData<>("");
    private final MutableLiveData<String> blockDescription = new MutableLiveData<>("");
    private final MutableLiveData<String> selectedColor = new MutableLiveData<>("#31628D");

    // Estados de la UI
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> message = new MutableLiveData<>();

    // Navegación post-creación exitosa
    private final MutableLiveData<Integer> shouldNavigateToDeck = new MutableLiveData<>();
    public LiveData<Integer> getShouldNavigateToDeck() { return shouldNavigateToDeck; }

    /**
     * Constructor que inicializa el ViewModel con el Repository.
     * Configura observador para procesar resultados de creación.
     */
    public DeckCreationViewModel(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;

        // Observar resultado de creación para navegación y feedback
        this.deckRepository.getDeckCreationResult().observeForever(result -> {
            isLoading.postValue(false);

            if (result.isSuccess()) {
                shouldNavigateToDeck.postValue(result.getDeck().getId());
            } else {
                message.postValue("Error: " + result.getMessage());
            }
        });
    }

    // Getters y setters para los parámetros de entrada
    public LiveData<String> getDiscipline() { return discipline; }
    public void setDiscipline(String value) { discipline.setValue(value); }

    public LiveData<String> getBlockDescription() { return blockDescription; }
    public void setBlockDescription(String value) { blockDescription.setValue(value); }

    public LiveData<String> getSelectedColor() { return selectedColor; }
    public void setSelectedColor(String value) { selectedColor.setValue(value); }

    // Getters para estados de la UI
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<DeckResponse> getDeckCreationResult() { return deckRepository.getDeckCreationResult(); }

    /**
     * Limpia el mensaje actual para evitar que se muestre nuevamente.
     */
    public void clearMessage() {
        message.setValue(null);
    }

    /**
     * Valida los campos y crea una nueva baraja si todo es correcto.
     */
    public void createDeck() {
        // Validar campos obligatorios
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

        // Iniciar proceso de creación
        isLoading.setValue(true);

        DeckCreationRequest request = new DeckCreationRequest(
                discipline.getValue(),
                blockDescription.getValue(),
                selectedColor.getValue()
        );

        deckRepository.createDeck(request);
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
            if (modelClass.isAssignableFrom(DeckCreationViewModel.class)) {
                return (T) new DeckCreationViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}