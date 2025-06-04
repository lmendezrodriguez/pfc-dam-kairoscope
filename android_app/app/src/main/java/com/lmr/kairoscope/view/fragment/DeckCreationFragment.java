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
import com.lmr.kairoscope.data.repository.DeckRepository;
import com.lmr.kairoscope.viewmodel.DeckCreationViewModel;

import java.util.ArrayList;
import java.util.List;

public class DeckCreationFragment extends Fragment {

    private static final String TAG = "DeckCreationFragment";
    private List<String> selectedTags = new ArrayList<>();

    // UI Elements
    private TextInputEditText editTextDiscipline;
    private ChipGroup chipGroupBlockTags;
    private GridLayout gridLayoutColors;
    private View buttonCreateDeck;
    private CircularProgressIndicator progressBar;

    // ViewModel
    private DeckCreationViewModel viewModel;

    // NavController
    private NavController navController;

    // Color seleccionado
    private String selectedColor = "#e24939";
    private View selectedColorView = null;

    // Colores disponibles
    private final String[] colorOptions = {
            "#e24939", "#f3e7cd", "#002fa7", "#5a6576", "#7fa87c",
            "#f4a73f", "#9b7fb8", "#4a9b8e", "#d66b7a", "#3c434f"
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deck_creation, container, false);

        // Obtener referencias a las vistas
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

        DeckRepository repository = new DeckRepository(requireContext());
        viewModel = new ViewModelProvider(this, new DeckCreationViewModel.Factory(repository))
                .get(DeckCreationViewModel.class);

        setupObservers();
        setupColorCircles();
        setupBlockTagChips();

        buttonCreateDeck.setOnClickListener(v -> createDeck());
    }

    private void setupObservers() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            editTextDiscipline.setEnabled(!isLoading);
            gridLayoutColors.setEnabled(!isLoading);
            buttonCreateDeck.setEnabled(!isLoading);
        });

        viewModel.getShouldNavigateToList().observe(getViewLifecycleOwner(), shouldNavigate -> {
            if (shouldNavigate != null && shouldNavigate) {
                navController.navigate(R.id.action_deckCreationFragment_to_deckListFragment);
            }
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                viewModel.clearMessage();
            }
        });

        viewModel.getDeckCreationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                Toast.makeText(requireContext(),
                        "¡Baraja '" + result.getDeck().getName() + "' creada!",
                        Toast.LENGTH_SHORT).show();
                clearFields();
            }
        });
    }

    private void setupColorCircles() {
        int circleSize = (int) (40 * getResources().getDisplayMetrics().density); // 40dp en px
        int margin = (int) (12 * getResources().getDisplayMetrics().density); // 12dp en px

        for (int i = 0; i < colorOptions.length; i++) {
            View colorCircle = new View(requireContext());

            // Configurar parámetros para GridLayout
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = circleSize;
            params.height = circleSize;
            params.setMargins(margin, margin, margin, margin);
            colorCircle.setLayoutParams(params);

            // Crear drawable circular
            GradientDrawable drawable = new GradientDrawable();
            drawable.setShape(GradientDrawable.OVAL);
            try {
                drawable.setColor(Color.parseColor(colorOptions[i]));
            } catch (Exception e) {
                drawable.setColor(Color.GRAY);
            }

            colorCircle.setBackground(drawable);

            final String color = colorOptions[i];

            colorCircle.setOnClickListener(v -> {
                selectColor(colorCircle, color);
            });

            gridLayoutColors.addView(colorCircle);

            // Seleccionar el primer color por defecto
            if (i == 0) {
                selectColor(colorCircle, color);
            }
        }
    }

    private void selectColor(View colorView, String color) {
        // Deseleccionar el anterior
        if (selectedColorView != null) {
            updateCircleSelection(selectedColorView, false);
        }

        // Seleccionar el nuevo
        selectedColorView = colorView;
        selectedColor = color;
        updateCircleSelection(colorView, true);

        viewModel.setSelectedColor(selectedColor);
    }

    private void updateCircleSelection(View colorView, boolean isSelected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);

        // Obtener el color de fondo
        String color = selectedColor;
        if (!isSelected) {
            // Encontrar el color de esta vista
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

        if (isSelected) {
            // Agregar borde para indicar selección
            drawable.setStroke(8, Color.parseColor("#1a1a2e")); // Color primario, borde más grueso
        }

        colorView.setBackground(drawable);
    }

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

    private void updateBlockDescription() {
        String description = selectedTags.isEmpty() ?
                "Bloqueo creativo general" :
                String.join(", ", selectedTags);

        viewModel.setBlockDescription(description);
    }

    private void clearFields() {
        editTextDiscipline.setText("");

        // Limpiar chips de bloqueo seleccionados
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        editTextDiscipline = null;
        chipGroupBlockTags = null;
        gridLayoutColors = null;
        buttonCreateDeck = null;
        progressBar = null;
        selectedTags.clear();
        selectedColorView = null;
    }
}