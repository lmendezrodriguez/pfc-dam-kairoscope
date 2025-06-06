package com.lmr.kairoscope.view.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import com.lmr.kairoscope.R;
import com.lmr.kairoscope.data.repository.AuthRepository;
import com.lmr.kairoscope.viewmodel.AuthViewModel;

import java.util.Objects;

/**
 * Fragment de registro que permite crear nuevas cuentas con Firebase Auth.
 * Incluye validación de campos y confirmación de contraseña.
 */
public class RegisterFragment extends Fragment {

    private static final String TAG = "RegisterFragment";

    // Referencias UI
    private TextInputEditText editTextName;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private TextInputEditText editTextRepeatPassword;
    private MaterialButton buttonRegister;
    private MaterialButton buttonLogin;
    private CircularProgressIndicator progressBar;

    // ViewModel y navegación
    private AuthViewModel authViewModel;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        // Inicializar referencias UI
        editTextName = view.findViewById(R.id.editTextName);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        editTextRepeatPassword = view.findViewById(R.id.editTextRepeatPassword);
        buttonRegister = view.findViewById(R.id.buttonRegister);
        buttonLogin = view.findViewById(R.id.buttonLogin);
        progressBar = view.findViewById(R.id.progressBar);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = NavHostFragment.findNavController(this);

        // Configurar ViewModel con su factory
        AuthRepository authRepository = new AuthRepository(requireContext());
        authViewModel = new ViewModelProvider(this, new AuthViewModel.Factory(authRepository))
                .get(AuthViewModel.class);

        setupObservers();
        setupClickListeners();
    }

    /**
     * Configura los observadores del ViewModel para actualizar la UI.
     */
    private void setupObservers() {
        // Observar estado de carga y gestionar habilitación de controles
        authViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "isLoading updated: " + isLoading);

            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);

            // Controlar interacción durante carga
            boolean enabled = !isLoading;
            buttonRegister.setEnabled(enabled);
            buttonLogin.setEnabled(enabled);
            editTextName.setEnabled(enabled);
            editTextEmail.setEnabled(enabled);
            editTextPassword.setEnabled(enabled);
            editTextRepeatPassword.setEnabled(enabled);
        });

        // Observar mensajes para feedback al usuario
        authViewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            Log.d(TAG, "Message received: " + message);

            if (message != null && !message.isEmpty()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
            }
        });

        // Observar estado de autenticación para navegación automática post-registro
        authViewModel.isAuthenticated().observe(getViewLifecycleOwner(), isAuthenticated -> {
            Log.d(TAG, "isAuthenticated updated: " + isAuthenticated);

            if (isAuthenticated != null && isAuthenticated) {
                try {
                    navController.navigate(R.id.action_registerFragment_to_homeFragment);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Navigation error to Home after registration: " + e.getMessage());
                }
            }
        });

        // Observar resultado completo de autenticación para logging
        authViewModel.getAuthResult().observe(getViewLifecycleOwner(), result -> {
            Log.d(TAG, "AuthResult received: " + (result != null ?
                    "Success: " + result.isSuccess() + ", Error: " + result.getErrorMessage() : "null"));
        });
    }

    /**
     * Configura los listeners de los botones.
     */
    private void setupClickListeners() {
        // Botón de registro con validación completa
        buttonRegister.setOnClickListener(v -> {
            String name = Objects.requireNonNull(editTextName.getText()).toString().trim();
            String email = Objects.requireNonNull(editTextEmail.getText()).toString().trim();
            String password = Objects.requireNonNull(editTextPassword.getText()).toString().trim();
            String repeatPassword = Objects.requireNonNull(editTextRepeatPassword.getText()).toString().trim();

            // Validar campos obligatorios
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
                Snackbar.make(requireView(), "Por favor, completa todos los campos", Snackbar.LENGTH_SHORT).show();
            } else if (!password.equals(repeatPassword)) {
                Snackbar.make(requireView(), "Las contraseñas no coinciden", Snackbar.LENGTH_SHORT).show();
            } else {
                authViewModel.register(email, password, name);
            }
        });

        // Botón de navegación a login
        buttonLogin.setOnClickListener(v -> {
            try {
                navController.navigate(R.id.action_registerFragment_to_loginFragment);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Navigation error to Login: " + e.getMessage());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Prevenir memory leaks limpiando referencias
        editTextName = null;
        editTextEmail = null;
        editTextPassword = null;
        editTextRepeatPassword = null;
        buttonRegister = null;
        buttonLogin = null;
        progressBar = null;
        navController = null;
    }
}