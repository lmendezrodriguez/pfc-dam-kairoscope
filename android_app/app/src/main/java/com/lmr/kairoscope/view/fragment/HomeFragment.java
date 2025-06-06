package com.lmr.kairoscope.view.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.lmr.kairoscope.R;
import com.lmr.kairoscope.data.repository.AuthRepository;
import com.lmr.kairoscope.data.repository.DeckRepository;
import com.lmr.kairoscope.viewmodel.HomeViewModel;

/**
 * Fragment principal que muestra un saludo personalizado y la última baraja creada.
 * Permite acceso rápido al detalle de la baraja más reciente.
 */
public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    // UI Elements
    private TextView textViewUserName;
    private TextView textViewWelcome;
    private androidx.cardview.widget.CardView cardViewDeck;
    private TextView textViewDeckName;
    private TextView textViewDiscipline;
    private TextView textViewEmptyMessage;
    private CircularProgressIndicator progressBar;
    private ImageView imageViewLogo;

    // ViewModel
    private HomeViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupViewModel();
        setupObservers();

        // Cargar datos iniciales
        viewModel.loadHomeData();
    }

    /**
     * Inicializa las referencias a los elementos de la UI.
     */
    private void initViews(View view) {
        textViewUserName = view.findViewById(R.id.textViewUserName);
        textViewWelcome = view.findViewById(R.id.textViewWelcome);
        cardViewDeck = view.findViewById(R.id.cardViewDeck);
        textViewDeckName = view.findViewById(R.id.textViewDeckName);
        textViewDiscipline = view.findViewById(R.id.textViewDiscipline);
        textViewEmptyMessage = view.findViewById(R.id.textViewEmptyMessage);
        progressBar = view.findViewById(R.id.progressBar);
        imageViewLogo = view.findViewById(R.id.imageViewLogo);
    }

    /**
     * Configura el ViewModel con sus dependencias.
     */
    private void setupViewModel() {
        AuthRepository authRepository = new AuthRepository(requireContext());
        DeckRepository deckRepository = new DeckRepository(requireContext());

        viewModel = new ViewModelProvider(this, new HomeViewModel.Factory(authRepository, deckRepository))
                .get(HomeViewModel.class);
    }

    /**
     * Configura los observadores para actualizar la UI según los cambios del ViewModel.
     */
    private void setupObservers() {
        // Observar perfil del usuario para personalizar el saludo
        viewModel.getCurrentUserProfile().observe(getViewLifecycleOwner(), userProfile -> {
            if (userProfile != null && userProfile.getDisplayName() != null) {
                textViewUserName.setText("¡Hola, " + userProfile.getDisplayName() + "!");
            } else {
                textViewUserName.setText("¡Hola!");
            }
        });

        // Observar estado de carga
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observar mensajes
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });

        // Observar última baraja y alternar entre estados
        viewModel.getLatestDeck().observe(getViewLifecycleOwner(), deck -> {
            if (deck != null) {
                showLatestDeck(deck);
            } else {
                showEmptyState();
            }
        });
    }

    /**
     * Muestra la información de la última baraja creada.
     */
    private void showLatestDeck(com.lmr.kairoscope.data.model.Deck deck) {
        cardViewDeck.setVisibility(View.VISIBLE);
        textViewEmptyMessage.setVisibility(View.GONE);
        textViewDeckName.setVisibility(View.VISIBLE);
        textViewDiscipline.setVisibility(View.VISIBLE);

        textViewDeckName.setText(deck.getName());
        textViewDiscipline.setText(deck.getDiscipline());

        // Aplicar color personalizado de la baraja
        try {
            int color = Color.parseColor(deck.getChosen_color());
            cardViewDeck.setCardBackgroundColor(color);
        } catch (Exception e) {
            cardViewDeck.setCardBackgroundColor(getResources().getColor(R.color.md_theme_primary));
        }

        // Configurar navegación al detalle al hacer clic
        cardViewDeck.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putInt("deck_id", deck.getId());
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_homeFragment_to_deckDetailFragment, args);
        });
    }

    /**
     * Muestra el estado cuando no hay barajas disponibles.
     */
    private void showEmptyState() {
        cardViewDeck.setVisibility(View.VISIBLE);
        textViewEmptyMessage.setVisibility(View.VISIBLE);
        textViewDeckName.setVisibility(View.GONE);
        textViewDiscipline.setVisibility(View.GONE);
        imageViewLogo.setVisibility(View.GONE);

        // Aplicar color por defecto
        cardViewDeck.setCardBackgroundColor(getResources().getColor(R.color.md_theme_primary));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Prevenir memory leaks limpiando referencias
        textViewUserName = null;
        textViewWelcome = null;
        cardViewDeck = null;
        textViewDeckName = null;
        textViewDiscipline = null;
        textViewEmptyMessage = null;
        progressBar = null;
        imageViewLogo = null;
    }
}