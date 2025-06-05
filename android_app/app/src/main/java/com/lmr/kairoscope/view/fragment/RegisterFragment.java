// android_app/app/src/main/java/com/lmr/kairoscope/view/fragment/RegisterFragment.java
// (ASEGÚRATE de que el nombre del paquete aquí arriba coincide con la ubicación real del archivo)

package com.lmr.kairoscope.view.fragment;

import android.content.Context;
import android.os.Bundle;
import android.util.Log; // Importa la clase Log para los mensajes de depuración
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
import com.google.android.material.snackbar.Snackbar; // Importa Snackbar
import com.google.android.material.textfield.TextInputEditText; // Importa TextInputEditText
import com.google.android.material.progressindicator.CircularProgressIndicator; // Importa CircularProgressIndicator

import com.lmr.kairoscope.R; // Importa la clase R para referenciar recursos
import com.lmr.kairoscope.data.repository.AuthRepository; // Importa tu Repository (para Factory)
import com.lmr.kairoscope.viewmodel.AuthViewModel; // Importa tu ViewModel

import java.util.Objects;


public class RegisterFragment extends Fragment {

    // Referencias a los elementos UI del layout (variables de instancia)
    private TextInputEditText editTextName; // Campo de Nombre
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private TextInputEditText editTextRepeatPassword;
    private MaterialButton buttonRegister; // Botón de Registrarse
    private MaterialButton buttonLogin; // Botón para ir a Iniciar Sesión
    private CircularProgressIndicator progressBar;

    // Referencia al ViewModel
    private AuthViewModel authViewModel;

    // Referencia al NavController
    private NavController navController;

    // Etiqueta para los logs de depuración
    private static final String TAG = "RegisterFragment";


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        // Obtener referencias a los elementos UI usando findViewById
        editTextName = view.findViewById(R.id.editTextName); // Obtener referencia al campo Nombre
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        editTextRepeatPassword = view.findViewById(R.id.editTextRepeatPassword);
        buttonRegister = view.findViewById(R.id.buttonRegister); // Obtener referencia al botón de Registrarse
        buttonLogin = view.findViewById(R.id.buttonLogin); // Obtener referencia al botón para ir a Login
        progressBar = view.findViewById(R.id.progressBar);

        // Devuelve la vista inflada. La configuración de ViewModel y observers se hará en onViewCreated.
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Obtener la instancia del NavController para navegar
        navController = NavHostFragment.findNavController(this);

        // Obtener la instancia del ViewModel usando el Factory.
        // Instanciamos el Repository aquí. En un proyecto más grande, usarías Inyección de Dependencias.
        AuthRepository authRepository = new AuthRepository(requireContext());
        authViewModel = new ViewModelProvider(this, new AuthViewModel.Factory(authRepository)).get(AuthViewModel.class);


        // --- Observar LiveData del ViewModel (Misma lógica que en LoginFragment) ---

        // Observar el estado de carga para mostrar/ocultar el ProgressBar y deshabilitar UI
        authViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            Log.d(TAG, "isLoading updated: " + isLoading);

            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
                // Deshabilitar interacción UI durante la carga
                buttonRegister.setEnabled(false);
                buttonLogin.setEnabled(false);
                editTextName.setEnabled(false); // Deshabilitar campo Nombre
                editTextEmail.setEnabled(false);
                editTextPassword.setEnabled(false);
                editTextRepeatPassword.setEnabled(false);
            } else {
                progressBar.setVisibility(View.GONE); // Oculta la barra de carga
                // Habilitar interacción UI una vez terminada la carga
                buttonRegister.setEnabled(true);
                buttonLogin.setEnabled(true);
                editTextName.setEnabled(true); // Habilitar campo Nombre
                editTextEmail.setEnabled(true);
                editTextPassword.setEnabled(true);
                editTextRepeatPassword.setEnabled(true);
            }
        });

        // Observar los mensajes del ViewModel para mostrarlos en un Snackbar
        authViewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            Log.d(TAG, "Message received: " + message);

            if (message != null && !message.isEmpty()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                // authViewModel.clearMessage(); // Llama a este método si lo implementaste en AuthViewModel
            }
        });

        // Observar el estado de autenticación para navegar si el usuario está logueado (registro exitoso)
        authViewModel.isAuthenticated().observe(getViewLifecycleOwner(), isAuthenticated -> {
            Log.d(TAG, "isAuthenticated updated: " + isAuthenticated);

            if (isAuthenticated != null && isAuthenticated) {
                // El usuario está autenticado (registro exitoso y logueo automático).
                // Navegar a la pantalla principal (DeckListFragment).
                try {
                    // *** IMPORTANTE: Asegúrate que R.id.action_registerFragment_to_deckListFragment es el ID correcto en tu nav_graph.xml ***
                    navController.navigate(R.id.action_registerFragment_to_homeFragment);
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Navigation error to DeckList after registration: " + e.getMessage());
                }
            }
            // Si isAuthenticated es false, el registro falló o el usuario no está logueado, permanecemos en esta pantalla.
        });

        // Observar authResult (Opcional: similar a LoginFragment)
        authViewModel.getAuthResult().observe(getViewLifecycleOwner(), result -> {
            Log.d(TAG, "AuthResult received: " + (result != null ? "Success: " + result.isSuccess() + ", Error: " + result.getErrorMessage() : "null"));
            // La lógica para manejar el resultado (mostrar mensajes, cambiar isLoading, navegar si es éxito)
            // ya está delegada a los otros observers (message, isLoading, isAuthenticated).
            // Puedes añadir lógica adicional aquí si la necesitas.
        });


        // --- Configurar Click Listeners ---

        // Click Listener para el botón de Registrarse
        buttonRegister.setOnClickListener(v -> {
            String name = Objects.requireNonNull(editTextName.getText()).toString().trim(); // Obtener el nombre
            String email = Objects.requireNonNull(editTextEmail.getText()).toString().trim();
            String password = Objects.requireNonNull(editTextPassword.getText()).toString().trim();
            String repeatPassword = Objects.requireNonNull(editTextRepeatPassword.getText()).toString().trim();

            // Validación básica de campos (que no estén vacíos)
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || repeatPassword.isEmpty()) {
                Snackbar.make(requireView(), "Por favor, completa todos los campos (Nombre, Email, Password)", Snackbar.LENGTH_SHORT).show();
            } else if (!password.equals(repeatPassword)) {
                Snackbar.make(requireView(), "Las contraseñas no coinciden", Snackbar.LENGTH_SHORT).show();
            } else {
                authViewModel.register(email, password, name);
            }
        });

        // Click Listener para el botón "Ya tengo cuenta" (ir a Login)
        buttonLogin.setOnClickListener(v -> {
            // NAVEGAR a LoginFragment usando NavController
            try {
                // *** IMPORTANTE: Asegúrate que R.id.action_registerFragment_to_loginFragment es el ID correcto en tu nav_graph.xml ***
                navController.navigate(R.id.action_registerFragment_to_loginFragment);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Navigation error to Login: " + e.getMessage());
            }
        });
    }

    // Limpiar referencias UI en onDestroyView
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Poner las referencias UI a null
        editTextName = null;
        editTextEmail = null;
        editTextPassword = null;
        buttonRegister = null;
        buttonLogin = null;
        progressBar = null;
        navController = null;
        editTextRepeatPassword = null;
    }
}