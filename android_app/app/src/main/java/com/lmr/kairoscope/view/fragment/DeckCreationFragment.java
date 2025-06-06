package com.lmr.kairoscope.view.fragment;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import com.lmr.kairoscope.R;
import com.lmr.kairoscope.data.model.DeckListResponse;
import com.lmr.kairoscope.data.repository.DeckRepository;
import com.lmr.kairoscope.viewmodel.DeckCreationViewModel;
import com.lmr.kairoscope.viewmodel.DeckListViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragmento para crear nuevas barajas de estrategias personalizadas.
 * Permite seleccionar disciplina, tipos de bloqueo y color temático.
 */
public class DeckCreationFragment extends Fragment {

    private static final String TAG = "DeckCreationFragment";
    private List<String> selectedTags = new ArrayList<>();
    private DeckListViewModel deckListViewModel;

    // UI Elements
    private TextInputEditText editTextDiscipline;
    private ChipGroup chipGroupBlockTags;
    private GridLayout gridLayoutColors;
    private View buttonCreateDeck;
    private CircularProgressIndicator progressBar;

    // ViewModel y navegación
    private DeckCreationViewModel viewModel;
    private NavController navController;

    // Gestión de selección de color
    private String selectedColor = "#e24939";
    private View selectedColorView = null;

    // Paleta de colores disponibles
    private final String[] colorOptions = {
            "#e24939", "#f3e7cd", "#002fa7", "#5a6576", "#7fa87c",
            "#f4a73f", "#9b7fb8", "#4a9b8e", "#d66b7a", "#3c434f"
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deck_creation, container, false);

        // Inicializar ViewModel para verificar límite de barajas
        DeckRepository deckRepository = new DeckRepository(requireContext());
        deckListViewModel = new ViewModelProvider(requireActivity(), new DeckListViewModel.Factory(deckRepository))
                .get(DeckListViewModel.class);

        deckListViewModel.loadDeckList();

        // Obtener referencias UI
        editTextDiscipline = view.findViewById(R.id.editTextDiscipline);
        chipGroupBlockTags = view.findViewById(R.id.chipGroupBlockTags);
        gridLayoutColors = view.findViewById(R.id.gridLayoutColors);
        buttonCreateDeck = view.findViewById(R.id.buttonCreateDeck);
        progressBar = view.findViewById(R.id.progressBar);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Inicializar ViewModel de creación
        DeckRepository repository = new DeckRepository(requireContext());
        viewModel = new ViewModelProvider(this, new DeckCreationViewModel.Factory(repository))
                .get(DeckCreationViewModel.class);

        setupObservers();
        setupColorCircles();
        setupBlockTagChips();

        // Configurar listeners
        buttonCreateDeck.setOnClickListener(v -> createDeck());
        editTextDiscipline.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                checkDeckLimit();
            }
        });
        editTextDiscipline.setOnClickListener(v -> checkDeckLimit());
    }

    /**
     * Configura los observadores LiveData del ViewModel.
     */
    private void setupObservers() {
        // Estado de carga
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            setFormEnabled(!isLoading);
        });

        // Navegación después de crear baraja
        viewModel.getShouldNavigateToDeck().observe(getViewLifecycleOwner(), deckId -> {
            if (deckId != null) {
                Bundle args = new Bundle();
                args.putInt("deck_id", deckId);
                navController.navigate(R.id.action_deckCreationFragment_to_deckDetailFragment, args);
            }
        });

        // Mensajes de estado
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                viewModel.clearMessage();
            }
        });

        // Resultado de creación de baraja
        viewModel.getDeckCreationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                Toast.makeText(requireContext(),
                        "¡Baraja '" + result.getDeck().getName() + "' creada!",
                        Toast.LENGTH_SHORT).show();
                clearFields();
            }
        });
    }

    /**
     * Crea dinámicamente los círculos de colores para selección.
     */
    private void setupColorCircles() {
        int circleSize = (int) (40 * getResources().getDisplayMetrics().density);
        int margin = (int) (12 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < colorOptions.length; i++) {
            View colorCircle = createColorCircle(circleSize, margin, colorOptions[i]);
            gridLayoutColors.addView(colorCircle);

            // Seleccionar primer color por defecto
            if (i == 0) {
                selectColor(colorCircle, colorOptions[i]);
            }
        }
    }

    /**
     * Crea un círculo de color individual.
     */
    private View createColorCircle(int size, int margin, String color) {
        View colorCircle = new View(requireContext());

        // Configurar layout
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = size;
        params.height = size;
        params.setMargins(margin, margin, margin, margin);
        colorCircle.setLayoutParams(params);

        // Crear drawable circular
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        try {
            drawable.setColor(Color.parseColor(color));
        } catch (Exception e) {
            drawable.setColor(Color.GRAY);
        }

        colorCircle.setBackground(drawable);
        colorCircle.setOnClickListener(v -> selectColor(colorCircle, color));

        return colorCircle;
    }

    /**
     * Selecciona un color y actualiza la UI.
     */
    private void selectColor(View colorView, String color) {
        // Deseleccionar anterior
        if (selectedColorView != null) {
            updateCircleSelection(selectedColorView, false);
        }

        // Seleccionar nuevo
        selectedColorView = colorView;
        selectedColor = color;
        updateCircleSelection(colorView, true);

        viewModel.setSelectedColor(selectedColor);
    }

    /**
     * Actualiza la apariencia visual del círculo de color (seleccionado/no seleccionado).
     */
    private void updateCircleSelection(View colorView, boolean isSelected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);

        // Determinar color de fondo
        String color = selectedColor;
        if (!isSelected) {
            int index = gridLayoutColors.indexOfChild(colorView);
            if (index >= 0 && index < colorOptions.length) {
                color = colorOptions[index];
            }
        }

        try {
            drawable.setColor(Color.parseColor(color));
        } catch (Exception e) {
            drawable.setColor(Color.GRAY);
        }

        // Añadir borde si está seleccionado
        if (isSelected) {
            drawable.setStroke(8, Color.parseColor("#1a1a2e"));
        }

        colorView.setBackground(drawable);
    }

    /**
     * Valida datos y crea una nueva baraja.
     */
    private void createDeck() {
        String discipline = editTextDiscipline.getText().toString().trim();

        if (discipline.isEmpty()) {
            Snackbar.make(requireView(), "Por favor, ingresa una disciplina", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (selectedTags.size() > 5) {
            Snackbar.make(requireView(), "Máximo 5 tipos de bloqueo permitidos", Snackbar.LENGTH_SHORT).show();
            return;
        }

        viewModel.setDiscipline(discipline);
        viewModel.setSelectedColor(selectedColor);
        viewModel.createDeck();
    }

    /**
     * Configura los chips para seleccionar tipos de bloqueo creativo.
     */
    private void setupBlockTagChips() {
        String[] tagArray = getResources().getStringArray(R.array.creative_block_tags);

        for (String tag : tagArray) {
            Chip chip = new Chip(requireContext());
            chip.setText(tag);
            chip.setCheckable(true);
            chip.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                if (isChecked) {
                    if (selectedTags.size() >= 5) {
                        chip.setChecked(false);
                        Snackbar.make(requireView(), "Máximo 5 tipos de bloqueo", Snackbar.LENGTH_SHORT).show();
                        return;
                    }
                    selectedTags.add(tag);
                } else {
                    selectedTags.remove(tag);
                }
                updateBlockDescription();
            });
            chipGroupBlockTags.addView(chip);
        }
    }

    /**
     * Actualiza la descripción del bloqueo basada en chips seleccionados.
     */
    private void updateBlockDescription() {
        String description = selectedTags.isEmpty() ?
                "Bloqueo creativo general" :
                String.join(", ", selectedTags);

        viewModel.setBlockDescription(description);
    }

    /**
     * Limpia todos los campos del formulario.
     */
    private void clearFields() {
        editTextDiscipline.setText("");

        // Limpiar chips seleccionados
        for (int i = 0; i < chipGroupBlockTags.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupBlockTags.getChildAt(i);
            chip.setChecked(false);
        }
        selectedTags.clear();

        // Resetear color al primero
        if (gridLayoutColors.getChildCount() > 0) {
            View firstCircle = gridLayoutColors.getChildAt(0);
            selectColor(firstCircle, colorOptions[0]);
        }

        updateBlockDescription();
    }

    /**
     * Verifica si el usuario ha alcanzado el límite de 8 barajas.
     */
    private void checkDeckLimit() {
        DeckListResponse response = deckListViewModel.getDeckListResult().getValue();
        if (response != null && response.isSuccess() && response.getDecks() != null) {
            if (response.getDecks().size() >= 8) {
                setFormEnabled(false);
                Snackbar.make(requireView(),
                        "Has alcanzado el límite de 8 barajas, borra una para poder generar una nueva",
                        Snackbar.LENGTH_LONG).show();
            } else {
                setFormEnabled(true);
            }
        }
    }

    /**
     * Habilita o deshabilita todos los elementos del formulario.
     */
    private void setFormEnabled(boolean enabled) {
        editTextDiscipline.setEnabled(enabled);
        chipGroupBlockTags.setEnabled(enabled);
        gridLayoutColors.setEnabled(enabled);
        buttonCreateDeck.setEnabled(enabled);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiar referencias para evitar memory leaks
        editTextDiscipline = null;
        chipGroupBlockTags = null;
        gridLayoutColors = null;
        buttonCreateDeck = null;
        progressBar = null;
        selectedTags.clear();
        selectedColorView = null;
        deckListViewModel = null;
    }
}