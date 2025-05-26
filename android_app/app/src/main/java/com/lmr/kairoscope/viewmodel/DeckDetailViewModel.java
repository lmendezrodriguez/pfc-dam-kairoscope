package com.lmr.kairoscope.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.lmr.kairoscope.data.model.Card;
import com.lmr.kairoscope.data.model.Deck;
import com.lmr.kairoscope.data.model.DeckDetailResponse;
import com.lmr.kairoscope.data.repository.DeckRepository;

import java.util.List;
import java.util.Random;

/**
 * ViewModel for managing deck detail data and card drawing logic.
 */
public class DeckDetailViewModel extends ViewModel {

    private final DeckRepository deckRepository;
    private final Random random = new Random();

    // Estado de carga
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // Mensajes para la UI
    private final MutableLiveData<String> message = new MutableLiveData<>();

    // Carta actualmente mostrada
    private final MutableLiveData<Card> currentCard = new MutableLiveData<>();

    // Deck actual
    private final MutableLiveData<Deck> currentDeck = new MutableLiveData<>();

    public DeckDetailViewModel(DeckRepository deckRepository) {
        this.deckRepository = deckRepository;

        // Observar resultado de deck detail
        this.deckRepository.getDeckDetailResult().observeForever(result -> {
            isLoading.postValue(false);

            if (result != null && result.isSuccess()) {
                currentDeck.postValue(result.getDeck());
                message.postValue("Baraja cargada correctamente");
            } else {
                message.postValue("Error al cargar la baraja");
            }
        });
    }

    // Getters para LiveData
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getMessage() { return message; }
    public LiveData<Card> getCurrentCard() { return currentCard; }
    public LiveData<Deck> getCurrentDeck() { return currentDeck; }
    public LiveData<DeckDetailResponse> getDeckDetailResult() { return deckRepository.getDeckDetailResult(); }

    // Método para cargar detalles de una baraja
    public void loadDeckDetail(int deckId) {
        isLoading.setValue(true);
        deckRepository.getDeckDetail(deckId);
    }

    // Método para sacar una carta aleatoria
    public void drawRandomCard() {
        Deck deck = currentDeck.getValue();
        if (deck != null && deck.getCards() != null && !deck.getCards().isEmpty()) {
            List<Card> cards = deck.getCards();
            int randomIndex = random.nextInt(cards.size());
            Card randomCard = cards.get(randomIndex);
            currentCard.setValue(randomCard);
        } else {
            message.setValue("No hay cartas disponibles");
        }
    }

    // Método para limpiar mensajes
    public void clearMessage() {
        message.setValue(null);
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
            if (modelClass.isAssignableFrom(DeckDetailViewModel.class)) {
                return (T) new DeckDetailViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}