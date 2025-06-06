package com.lmr.kairoscope.view.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

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

/**
 * Fragment de inicio de sesión que permite autenticación con Firebase.
 * Maneja la validación de credenciales y navegación post-login.
 */
public class LoginFragment extends Fragment {

    private static final String TAG = "LoginFragment";

    // Referencias UI
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private MaterialButton buttonLogin;
    private MaterialButton buttonRegister;
    private CircularProgressIndicator progressBar;

    // ViewModel y navegación
    private AuthViewModel authViewModel;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Inicializar referencias UI
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        buttonLogin = view.findViewById(R.id.buttonLogin);
        buttonRegister = view.findViewById(R.id.buttonRegister);
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

        // Verificar si ya hay sesión activa
        authViewModel.checkAuthenticationState();
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
            buttonLogin.setEnabled(enabled);
            buttonRegister.setEnabled(enabled);
            editTextEmail.setEnabled(enabled);
            editTextPassword.setEnabled(enabled);
        });

        // Observar mensajes para feedback al usuario
        authViewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            Log.d(TAG, "Message received: " + message);

            if (message != null && !message.isEmpty()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
            }
        });

        // Observar estado de autenticación para navegación automática
        authViewModel.isAuthenticated().observe(getViewLifecycleOwner(), isAuthenticated -> {
            Log.d(TAG, "isAuthenticated updated: " + isAuthenticated);

            if (isAuthenticated != null && isAuthenticated) {
                try {
                    navController.navigate(R.id.action_loginFragment_to_homeFragment);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Navigation error to Home: " + e.getMessage());
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
        // Botón de login con validación
        buttonLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(requireView(), "Por favor, ingresa email y contraseña", Snackbar.LENGTH_SHORT).show();
            } else {
                authViewModel.login(email, password);
            }
        });

        // Botón de navegación a registro
        buttonRegister.setOnClickListener(v -> {
            try {
                navController.navigate(R.id.action_loginFragment_to_registerFragment);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Navigation error to Register: " + e.getMessage());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Prevenir memory leaks limpiando referencias
        editTextEmail = null;
        editTextPassword = null;
        buttonLogin = null;
        buttonRegister = null;
        progressBar = null;
        navController = null;
    }
}