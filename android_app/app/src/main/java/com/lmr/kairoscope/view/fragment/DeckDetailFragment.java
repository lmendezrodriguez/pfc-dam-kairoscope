package com.lmr.kairoscope.view.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.lmr.kairoscope.R;
import com.lmr.kairoscope.data.repository.DeckRepository;
import com.lmr.kairoscope.viewmodel.DeckDetailViewModel;

public class DeckDetailFragment extends Fragment {

    private static final String TAG = "DeckDetailFragment";
    private static final String ARG_DECK_ID = "deck_id";

    // UI Elements
    private TextView textViewDeckName;
    private TextView textViewDiscipline;
    private TextView textViewCardText;
    private TextView textViewCardCount;
    private MaterialButton buttonDrawCard;
    private CircularProgressIndicator progressBar;

    // ViewModel
    private DeckDetailViewModel viewModel;

    // Data
    private int deckId;

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
        if (getArguments() != null) {
            deckId = getArguments().getInt(ARG_DECK_ID);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deck_detail, container, false);

        // Obtener referencias UI
        textViewDeckName = view.findViewById(R.id.textViewDeckName);
        textViewDiscipline = view.findViewById(R.id.textViewDiscipline);
        textViewCardText = view.findViewById(R.id.textViewCardText);
        textViewCardCount = view.findViewById(R.id.textViewCardCount);
        buttonDrawCard = view.findViewById(R.id.buttonDrawCard);
        progressBar = view.findViewById(R.id.progressBar);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar ViewModel
        DeckRepository repository = new DeckRepository(requireContext());
        viewModel = new ViewModelProvider(this, new DeckDetailViewModel.Factory(repository))
                .get(DeckDetailViewModel.class);

        // Configurar observadores
        setupObservers();

        // Configurar listeners
        setupListeners();

        // Cargar datos
        viewModel.loadDeckDetail(deckId);
    }

    private void setupObservers() {
        // Observar estado de carga
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            buttonDrawCard.setEnabled(!isLoading);
        });

        // Observar mensajes
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });

        // Observar deck actual
        viewModel.getCurrentDeck().observe(getViewLifecycleOwner(), deck -> {
            if (deck != null) {
                textViewDeckName.setText(deck.getName());
                textViewDiscipline.setText(deck.getDiscipline());
                textViewCardCount.setText(deck.getCard_count() + " cartas disponibles");

                // Aplicar color de la baraja si es válido
                try {
                    int color = Color.parseColor(deck.getChosen_color());
                    // Podrías aplicar el color a algún elemento visual
                } catch (Exception e) {
                    Log.w(TAG, "Invalid color format: " + deck.getChosen_color());
                }
            }
        });

        // Observar carta actual
        viewModel.getCurrentCard().observe(getViewLifecycleOwner(), card -> {
            if (card != null) {
                textViewCardText.setText(card.getText());
            }
        });
    }

    private void setupListeners() {
        // Botón para sacar carta
        buttonDrawCard.setOnClickListener(v -> {
            viewModel.drawRandomCard();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiar referencias
        textViewDeckName = null;
        textViewDiscipline = null;
        textViewCardText = null;
        textViewCardCount = null;
        buttonDrawCard = null;
        progressBar = null;
    }
}