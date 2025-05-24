package com.lmr.kairoscope.view.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.lmr.kairoscope.R;
import com.lmr.kairoscope.data.repository.DeckRepository;
import com.lmr.kairoscope.viewmodel.DeckCreationViewModel;

public class DeckCreationFragment extends Fragment {

    private static final String TAG = "DeckCreationFragment";

    // UI Elements
    private TextInputEditText editTextDiscipline;
    private TextInputEditText editTextBlockDescription;
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
    private int redValue = 49;  // Valores iniciales basados en #31628D
    private int greenValue = 98;
    private int blueValue = 141;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_deck_creation, container, false);

        // Obtener referencias a las vistas
        editTextDiscipline = view.findViewById(R.id.editTextDiscipline);
        editTextBlockDescription = view.findViewById(R.id.editTextBlockDescription);
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

        // Obtener NavController
        navController = NavHostFragment.findNavController(this);

        // Inicializar ViewModel
        DeckRepository repository = new DeckRepository();
        viewModel = new ViewModelProvider(this, new DeckCreationViewModel.Factory(repository))
                .get(DeckCreationViewModel.class);

        // Configurar observadores para los LiveData del ViewModel
        setupObservers();

        // Configurar los controles de color
        setupColorControls();

        // Configurar listener para el botón
        buttonCreateDeck.setOnClickListener(v -> {
            createDeck();
        });
    }

    private void setupObservers() {
        // Observar estado de carga
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            // Deshabilitar UI durante la carga
            editTextDiscipline.setEnabled(!isLoading);
            editTextBlockDescription.setEnabled(!isLoading);
            seekBarRed.setEnabled(!isLoading);
            seekBarGreen.setEnabled(!isLoading);
            seekBarBlue.setEnabled(!isLoading);
            buttonCreateDeck.setEnabled(!isLoading);
        });

        // Observar navegación a la lista de barajas
        viewModel.getShouldNavigateToList().observe(getViewLifecycleOwner(), shouldNavigate -> {
            if (shouldNavigate != null && shouldNavigate) {
                navController.navigate(R.id.action_deckCreationFragment_to_deckListFragment);
            }
        });

        // Observar mensajes
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                viewModel.clearMessage();
            }
        });

        // Observar resultado de creación de baraja
        viewModel.getDeckCreationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null && result.isSuccess()) {
                // Mostrar Toast con información de éxito
                Toast.makeText(requireContext(),
                        "¡Baraja '" + result.getDeck().getName() + "' creada!",
                        Toast.LENGTH_SHORT).show();

                // Aquí podríamos navegar a la lista de barajas o a la vista de detalle
                // navController.navigate(R.id.action_deckCreationFragment_to_deckListFragment);

                // O limpiar los campos para crear otra baraja
                clearFields();
            }
        });
    }

    private void setupColorControls() {
        // Configurar listeners para las barras de color
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
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No necesitamos hacer nada aquí
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No necesitamos hacer nada aquí
            }
        };

        // Asignar listeners a las barras
        seekBarRed.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBarGreen.setOnSeekBarChangeListener(seekBarChangeListener);
        seekBarBlue.setOnSeekBarChangeListener(seekBarChangeListener);

        // Configurar valores iniciales
        seekBarRed.setProgress(redValue);
        seekBarGreen.setProgress(greenValue);
        seekBarBlue.setProgress(blueValue);
        textViewRedValue.setText(String.valueOf(redValue));
        textViewGreenValue.setText(String.valueOf(greenValue));
        textViewBlueValue.setText(String.valueOf(blueValue));

        // Actualizar la vista previa del color
        updateColorPreview();
    }

    private void updateColorPreview() {
        // Crear el color RGB a partir de los valores
        int color = Color.rgb(redValue, greenValue, blueValue);

        // Actualizar la vista de vista previa
        viewSelectedColor.setBackgroundColor(color);

        // Actualizar el texto hexadecimal
        String hexColor = String.format("#%02X%02X%02X", redValue, greenValue, blueValue);
        textViewHexColor.setText(hexColor);

        // Actualizar el ViewModel
        viewModel.setSelectedColor(hexColor);
    }

    private void createDeck() {
        // Obtener valores actuales de los campos
        String discipline = editTextDiscipline.getText().toString().trim();
        String blockDescription = editTextBlockDescription.getText().toString().trim();

        // Actualizar ViewModel con los valores
        viewModel.setDiscipline(discipline);
        viewModel.setBlockDescription(blockDescription);

        // El color ya debe estar configurado en el ViewModel por los listeners

        // Solicitar la creación
        viewModel.createDeck();
    }

    private void clearFields() {
        editTextDiscipline.setText("");
        editTextBlockDescription.setText("");
        // No restauramos el color seleccionado, para mantener la preferencia del usuario
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiar referencias a las vistas
        editTextDiscipline = null;
        editTextBlockDescription = null;
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
    }
}