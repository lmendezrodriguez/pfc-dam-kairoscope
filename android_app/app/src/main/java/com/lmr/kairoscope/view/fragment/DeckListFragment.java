package com.lmr.kairoscope.view.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import com.lmr.kairoscope.R;
import com.lmr.kairoscope.adapters.DeckListAdapter;
import com.lmr.kairoscope.data.model.Deck;
import com.lmr.kairoscope.data.repository.DeckRepository;
import com.lmr.kairoscope.viewmodel.DeckListViewModel;

/**
 * Fragment que muestra la lista de barajas del usuario.
 * Permite navegar a la creación de nuevas barajas y al detalle de las existentes.
 */
public class DeckListFragment extends Fragment implements DeckListAdapter.OnDeckClickListener {

    private static final String TAG = "DeckListFragment";

    // UI Elements
    private RecyclerView recyclerViewDecks;
    private LinearLayout layoutEmptyState;
    private CircularProgressIndicator progressBar;

    // ViewModel y Adapter
    private DeckListViewModel viewModel;
    private DeckListAdapter adapter;

    // Navigation
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deck_list, container, false);

        // Obtener referencias UI
        recyclerViewDecks = view.findViewById(R.id.recyclerViewDecks);
        layoutEmptyState = view.findViewById(R.id.layoutEmptyState);
        progressBar = view.findViewById(R.id.progressBar);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Usar requireActivity() para compartir ViewModel entre fragments si es necesario
        DeckRepository repository = new DeckRepository(requireContext());
        viewModel = new ViewModelProvider(requireActivity(), new DeckListViewModel.Factory(repository))
                .get(DeckListViewModel.class);

        setupRecyclerView();
        setupObservers();

        // Cargar datos inicial
        viewModel.loadDeckList();
    }

    /**
     * Configura el RecyclerView con su adapter y layout manager.
     */
    private void setupRecyclerView() {
        adapter = new DeckListAdapter(this);
        recyclerViewDecks.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewDecks.setAdapter(adapter);
    }

    /**
     * Configura los observadores del ViewModel para actualizar la UI.
     */
    private void setupObservers() {
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

        // Observar lista de barajas y gestionar estados vacío/con datos
        viewModel.getDeckListResult().observe(getViewLifecycleOwner(), response -> {
            if (response != null && response.isSuccess()) {
                adapter.updateDeckList(response.getDecks());

                // Alternar entre lista y estado vacío
                if (response.getDecks() != null && !response.getDecks().isEmpty()) {
                    recyclerViewDecks.setVisibility(View.VISIBLE);
                    layoutEmptyState.setVisibility(View.GONE);
                } else {
                    recyclerViewDecks.setVisibility(View.GONE);
                    layoutEmptyState.setVisibility(View.VISIBLE);
                }
            } else {
                // Error - mostrar estado vacío
                recyclerViewDecks.setVisibility(View.GONE);
                layoutEmptyState.setVisibility(View.VISIBLE);
                adapter.updateDeckList(null);
            }
        });

        // Observar resultado de eliminación
        viewModel.getDeckDeleteResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                Snackbar.make(requireView(), result.getMessage(), Snackbar.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });
    }

    @Override
    public void onDeckClick(Deck deck) {
        // Navegar al detalle pasando el ID como argumento
        try {
            Bundle args = new Bundle();
            args.putInt("deck_id", deck.getId());
            navController.navigate(R.id.action_deckListFragment_to_deckDetailFragment, args);
        } catch (Exception e) {
            Log.e(TAG, "Navigation error to DeckDetail: " + e.getMessage());
            Snackbar.make(requireView(), "Error al abrir la baraja", Snackbar.LENGTH_SHORT).show();
            viewModel.clearMessage();
        }
    }

    @Override
    public void onDeckDelete(Deck deck) {
        // Confirmar eliminación con diálogo
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Eliminar baraja")
                .setMessage("¿Estás seguro de que quieres eliminar \"" + deck.getName() + "\"? Esta acción no se puede deshacer.")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    viewModel.deleteDeck(deck.getId());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Prevenir memory leaks limpiando referencias
        recyclerViewDecks = null;
        layoutEmptyState = null;
        progressBar = null;
        adapter = null;
    }
}