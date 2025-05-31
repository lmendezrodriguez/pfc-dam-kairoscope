package com.lmr.kairoscope.view.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;
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
    private View viewSelectedColor;
    private SeekBar seekBarRed;
    private SeekBar seekBarGreen;
    private SeekBar seekBarBlue;
    private TextView textViewRedValue;
    private TextView textViewGreenValue;
    private TextView textViewBlueValue;
    private TextView textViewHexColor;
    private View buttonCreateDeck;
    private CircularProgressIndicator progressBar;

    // ViewModel
    private DeckCreationViewModel viewModel;

    // NavController
    private NavController navController;

    // Valores RGB actuales
    private int redValue = 49;
    private int greenValue = 98;
    private int blueValue = 141;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deck_creation, container, false);

        // Obtener referencias a las vistas
        editTextDiscipline = view.findViewById(R.id.editTextDiscipline);
        chipGroupBlockTags = view.findViewById(R.id.chipGroupBlockTags);
        viewSelectedColor = view.findViewById(R.id.viewSelectedColor);
        seekBarRed = view.findViewById(R.id.seekBarRed);
        seekBarGreen = view.findViewById(R.id.seekBarGreen);
        seekBarBlue = view.findViewById(R.id.seekBarBlue);
        textViewRedValue = view.findViewById(R.id.textViewRedValue);
        textViewGreenValue = view.findViewById(R.id.textViewGreenValue);
        textViewBlueValue = view.findViewById(R.id.textViewBlueValue);
        textViewHexColor = view.findViewById(R.id.textViewHexColor);
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
        setupColorControls();
        setupBlockTagChips();

        buttonCreateDeck.setOnClickListener(v -> createDeck());
    }

    private void setupObservers() {
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            editTextDiscipline.setEnabled(!isLoading);
            seekBarRed.setEnabled(!isLoading);
            seekBarGreen.setEnabled(!isLoading);
            seekBarBlue.setEnabled(!isLoading);
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

    private void setupColorControls() {
        SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    if (seekBar.getId() == R.id.seekBarRed) {
                        redValue = progress;
                        textViewRedValue.setText(String.valueOf(progress));
                    } else if (seekBar.getId() == R.id.seekBarGreen) {
                        greenValue = progress;
                        textViewGreenValue.setText(String.valueOf(progress));
                    } else if (seekBar.getId() == R.id.seekBarBlue) {
                        blueValue = progress;
                        textViewBlueValue.setText(String.valueOf(progress));
                    }
                    updateColorPreview();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };

        seekBarRed.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBarGreen.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBarBlue.setOnSeekBarChangeListener(seekBarChangeListener);

        seekBarRed.setProgress(redValue);
        seekBarGreen.setProgress(greenValue);
        seekBarBlue.setProgress(blueValue);
        textViewRedValue.setText(String.valueOf(redValue));
        textViewGreenValue.setText(String.valueOf(greenValue));
        textViewBlueValue.setText(String.valueOf(blueValue));

        updateColorPreview();
    }

    private void updateColorPreview() {
        int color = Color.rgb(redValue, greenValue, blueValue);
        viewSelectedColor.setBackgroundColor(color);

        String hexColor = String.format("#%02X%02X%02X", redValue, greenValue, blueValue);
        textViewHexColor.setText(hexColor);

        viewModel.setSelectedColor(hexColor);
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
        // Limpiar chips seleccionados
        for (int i = 0; i < chipGroupBlockTags.getChildCount(); i++) {
            Chip chip = (Chip) chipGroupBlockTags.getChildAt(i);
            chip.setChecked(false);
        }
        selectedTags.clear();
        updateBlockDescription();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        editTextDiscipline = null;
        chipGroupBlockTags = null;
        viewSelectedColor = null;
        seekBarRed = null;
        seekBarGreen = null;
        seekBarBlue = null;
        textViewRedValue = null;
        textViewGreenValue = null;
        textViewBlueValue = null;
        textViewHexColor = null;
        buttonCreateDeck = null;
        progressBar = null;
        selectedTags.clear();
    }
}