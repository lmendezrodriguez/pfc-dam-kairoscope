package com.lmr.kairoscope.view.fragment;

import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.lmr.kairoscope.R;
import com.lmr.kairoscope.data.repository.DeckRepository;
import com.lmr.kairoscope.viewmodel.DeckDetailViewModel;

import java.util.Random;

/**
 * Fragment que muestra los detalles de una baraja específica con animación de cartas.
 * Implementa funcionalidad de flip para mostrar/ocultar el contenido de las cartas.
 */
public class DeckDetailFragment extends Fragment {

    private static final String TAG = "DeckDetailFragment";
    // Claves para conservar estado durante rotaciones
    private static final String STATE_CARD_TEXT = "current_card_text";
    private static final String STATE_CARD_LOADED = "card_loaded";
    private static final String ARG_DECK_ID = "deck_id";

    // Referencias UI
    private TextView textViewDeckName;
    private TextView textViewDiscipline;
    private TextView textViewCardText;
    private CircularProgressIndicator progressBar;
    private FrameLayout cardContainer;
    private androidx.cardview.widget.CardView cardViewBack;
    private androidx.cardview.widget.CardView cardViewFront;

    private DeckDetailViewModel viewModel;

    private int deckId;
    private boolean cardAlreadyLoaded = false;

    /**
     * Crea una nueva instancia del fragment con el ID de baraja especificado.
     */
    public static DeckDetailFragment newInstance(int deckId) {
        DeckDetailFragment fragment = new DeckDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_DECK_ID, deckId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Recuperar estado tras rotación o argumentos iniciales
        if (savedInstanceState != null) {
            deckId = savedInstanceState.getInt("deck_id", -1);
            cardAlreadyLoaded = savedInstanceState.getBoolean(STATE_CARD_LOADED, false);
        }
        else if (getArguments() != null) {
            deckId = getArguments().getInt(ARG_DECK_ID, -1);
        }

        // Validar que tenemos un ID válido
        if (deckId == -1) {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deck_detail, container, false);

        // Inicializar referencias UI
        textViewDeckName = view.findViewById(R.id.textViewDeckName);
        textViewDiscipline = view.findViewById(R.id.textViewDiscipline);
        textViewCardText = view.findViewById(R.id.textViewCardText);
        progressBar = view.findViewById(R.id.progressBar);
        cardContainer = view.findViewById(R.id.cardContainer);
        cardViewBack = view.findViewById(R.id.cardViewBack);
        cardViewFront = view.findViewById(R.id.cardViewFront);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configurar ViewModel con repositorio
        DeckRepository repository = new DeckRepository(requireContext());
        viewModel = new ViewModelProvider(this, new DeckDetailViewModel.Factory(repository))
                .get(DeckDetailViewModel.class);

        // Restaurar texto de carta tras rotación
        if (savedInstanceState != null) {
            String cardText = savedInstanceState.getString(STATE_CARD_TEXT);
            if (cardText != null && textViewCardText != null) {
                textViewCardText.setText(cardText);
            }
        }

        setupObservers();
        setupListeners();

        // Cargar datos de la baraja
        viewModel.loadDeckDetail(deckId);
    }

    /**
     * Configura observers para reaccionar a cambios en el ViewModel.
     */
    private void setupObservers() {
        // Observer para el estado de carga
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observer para mensajes de feedback al usuario
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });

        // Observer para datos de la baraja
        viewModel.getCurrentDeck().observe(getViewLifecycleOwner(), deck -> {
            if (deck != null) {
                textViewDeckName.setText(deck.getName());
                textViewDiscipline.setText(deck.getDiscipline());

                // Aplicar color personalizado de la baraja
                try {
                    int color = Color.parseColor(deck.getChosen_color());
                    // Podrías aplicar el color a algún elemento visual
                } catch (Exception e) {
                    Log.w(TAG, "Invalid color format: " + deck.getChosen_color());
                }

                // Sacar primera carta solo si no se había cargado antes
                if (!cardAlreadyLoaded) {
                    viewModel.drawRandomCard();
                    cardAlreadyLoaded = true;
                }
            }
        });

        // Observer para la carta actual
        viewModel.getCurrentCard().observe(getViewLifecycleOwner(), card -> {
            if (card != null) {
                textViewCardText.setText(card.getText());

                // Asignar color aleatorio al reverso de la carta
                TypedArray colors = getResources().obtainTypedArray(R.array.card_back_colors);
                int randomIndex = new Random().nextInt(colors.length());
                int randomColor = colors.getColor(randomIndex, getResources().getColor(R.color.md_theme_primary));
                colors.recycle();

                cardViewBack.setCardBackgroundColor(randomColor);
            }
        });
    }

    private void setupListeners() {
        setupCardFlip();
    }

    /**
     * Configura el listener para la animación de flip de cartas.
     */
    private void setupCardFlip() {
        cardContainer.setOnClickListener(v -> flipCard());
    }

    /**
     * Alterna entre mostrar el reverso y el contenido de la carta.
     */
    private void flipCard() {
        if (cardViewBack.getVisibility() == View.VISIBLE) {
            // Voltear a frontal (mostrar carta)
            flipToFront();
        } else {
            // Voltear a trasera y sacar nueva carta
            flipToBackAndNewCard();
        }
    }

    /**
     * Anima la transición del reverso al contenido de la carta.
     */
    private void flipToFront() {
        cardViewBack.animate()
                .rotationY(90f)
                .setDuration(200)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    cardViewBack.setVisibility(View.GONE);
                    cardViewFront.setVisibility(View.VISIBLE);
                    cardViewFront.setRotationY(-90f);
                    cardViewFront.animate()
                            .rotationY(0f)
                            .setDuration(200)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator());
                });
    }

    /**
     * Anima la transición al reverso y genera una nueva carta aleatoria.
     */
    private void flipToBackAndNewCard() {
        cardViewFront.animate()
                .rotationY(90f)
                .setDuration(200)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(() -> {
                    viewModel.drawRandomCard(); // Sacar nueva carta
                    cardViewFront.setVisibility(View.GONE);
                    cardViewBack.setVisibility(View.VISIBLE);
                    cardViewBack.setRotationY(-90f);
                    cardViewBack.animate()
                            .rotationY(0f)
                            .setDuration(200)
                            .setInterpolator(new android.view.animation.DecelerateInterpolator());
                });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Conservar estado para rotaciones
        outState.putInt("deck_id", deckId);
        outState.putBoolean(STATE_CARD_LOADED, cardAlreadyLoaded);

        if (textViewCardText != null) {
            outState.putString(STATE_CARD_TEXT, textViewCardText.getText().toString());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Evitar memory leaks limpiando referencias
        textViewDeckName = null;
        textViewDiscipline = null;
        textViewCardText = null;
        progressBar = null;
        cardContainer = null;
        cardViewBack = null;
        cardViewFront = null;
    }
}