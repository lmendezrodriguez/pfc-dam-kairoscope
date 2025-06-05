// android_app/app/src/main/java/com/lmr/kairoscope/view/fragment/LoginFragment.java
// (ASEGÚRATE de que el nombre del paquete aquí arriba coincide con la ubicación real del archivo)

package com.lmr.kairoscope.view.fragment;

import android.os.Bundle;
import android.util.Log; // Importa la clase Log para los mensajes de depuración
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button; // MaterialButton hereda de Button, esta importación es segura
import android.widget.ProgressBar; // CircularProgressIndicator hereda de ProgressBar

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


public class LoginFragment extends Fragment {

    // Referencias a los elementos UI del layout (variables de instancia para acceder desde onViewCreated)
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextPassword;
    private MaterialButton buttonLogin;
    private MaterialButton buttonRegister;
    private CircularProgressIndicator progressBar;

    // Referencia al ViewModel
    private AuthViewModel authViewModel;

    // Referencia al NavController
    private NavController navController;

    // Etiqueta para los logs de depuración
    private static final String TAG = "LoginFragment";


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        // Obtener referencias a los elementos UI usando findViewById
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        buttonLogin = view.findViewById(R.id.buttonLogin);
        buttonRegister = view.findViewById(R.id.buttonRegister);
        progressBar = view.findViewById(R.id.progressBar);

        // Devuelve la vista inflada. La configuración de ViewModel y observers se hará en onViewCreated.
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Obtener la instancia del NavController para navegar
        // findNavController es seguro de usar aquí porque onViewCreated se llama después de onCreateView
        navController = NavHostFragment.findNavController(this);

        // Obtener la instancia del ViewModel usando el Factory.
        // Instanciamos el Repository aquí. En un proyecto más grande, usarías Inyección de Dependencias (ej: Dagger Hilt).
        AuthRepository authRepository = new AuthRepository(requireContext());
        authViewModel = new ViewModelProvider(this, new AuthViewModel.Factory(authRepository)).get(AuthViewModel.class);


        // --- Observar LiveData del ViewModel ---

        // Observar el estado de carga para mostrar/ocultar el ProgressBar y deshabilitar botones/campos
        authViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Log de depuración para verificar cuándo se actualiza isLoading
            Log.d(TAG, "isLoading updated: " + isLoading);

            if (isLoading) {
                progressBar.setVisibility(View.VISIBLE);
                // Deshabilitar interacción UI durante la carga
                buttonLogin.setEnabled(false);
                buttonRegister.setEnabled(false);
                editTextEmail.setEnabled(false);
                editTextPassword.setEnabled(false);
            } else {
                progressBar.setVisibility(View.GONE); // <-- Esto es lo que oculta la barra de carga
                // Habilitar interacción UI una vez terminada la carga
                buttonLogin.setEnabled(true);
                buttonRegister.setEnabled(true);
                editTextEmail.setEnabled(true);
                editTextPassword.setEnabled(true);
            }
        });

        // Observar los mensajes del ViewModel para mostrarlos en un Snackbar
        authViewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            // Log de depuración para verificar si llega un mensaje
            Log.d(TAG, "Message received: " + message);

            if (message != null && !message.isEmpty()) {
                // Mostrar el mensaje usando un Snackbar. requireView() es más seguro que 'view' a veces.
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                // Si usas MutableLiveData simple, podrías necesitar limpiar el mensaje en el ViewModel
                // para evitar que reaparezca en recreaciones de vista.
                // authViewModel.clearMessage(); // Llama a este método si lo implementaste en AuthViewModel
            }
        });

        // Observar el estado de autenticación para navegar si el usuario está logueado
        authViewModel.isAuthenticated().observe(getViewLifecycleOwner(), isAuthenticated -> {
            // Log de depuración para verificar el estado de autenticación
            Log.d(TAG, "isAuthenticated updated: " + isAuthenticated);

            if (isAuthenticated != null && isAuthenticated) {
                // El usuario está autenticado. Navegar a la pantalla principal (DeckListFragment).
                // Usamos try-catch porque la navegación podría fallar si el fragmento ya no está en un estado válido.
                try {
                    // *** IMPORTANTE: Asegúrate que R.id.action_loginFragment_to_deckListFragment es el ID correcto en tu nav_graph.xml ***
                    navController.navigate(R.id.action_loginFragment_to_homeFragment);
                } catch (IllegalArgumentException e) {
                    // Esto puede ocurrir si intentas navegar de nuevo rápidamente o si el fragmento ya no está activo/visible
                    Log.e(TAG, "Navigation error to DeckList: " + e.getMessage());
                }
            }
            // Si isAuthenticated es false, el usuario no está logueado, permanecemos en esta pantalla.
            // Los mensajes de error específicos de login/registro ya se manejan en el observer de `message`.
        });

        // Observar authResult (Opcional: si necesitas lógica específica basada en el resultado completo de auth,
        // más allá del estado general y los mensajes ya manejados por otros observers)
        authViewModel.getAuthResult().observe(getViewLifecycleOwner(), result -> {
            // Log de depuración para verificar si llega un AuthResult
            Log.d(TAG, "AuthResult received: " + (result != null ? "Success: " + result.isSuccess() + ", Error: " + result.getErrorMessage() : "null"));
            // La lógica para manejar el resultado (mostrar mensajes, cambiar isLoading, navegar si es éxito)
            // ya está delegada a los otros observers (message, isLoading, isAuthenticated).
            // Puedes añadir lógica adicional aquí si la necesitas.
        });


        // --- Configurar Click Listeners ---

        // Click Listener para el botón de Login
        buttonLogin.setOnClickListener(v -> {
            String email = editTextEmail.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            // Validación básica de campos (que no estén vacíos)
            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(requireView(), "Por favor, ingresa email y contraseña", Snackbar.LENGTH_SHORT).show();
            } else {
                // Llamar al método del ViewModel para iniciar sesión
                authViewModel.login(email, password);
                // La barra de carga y el estado de los botones se actualizarán vía el observer de isLoading.
                // El resultado (éxito/fallo y mensaje) se comunicará vía los observers de isAuthenticated y message.
            }
        });

        // Click Listener para el botón de Registro
        buttonRegister.setOnClickListener(v -> {
            // NAVEGAR a RegisterFragment usando NavController
            try {
                // *** IMPORTANTE: Asegúrate que R.id.action_loginFragment_to_registerFragment es el ID correcto en tu nav_graph.xml ***
                navController.navigate(R.id.action_loginFragment_to_registerFragment);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "Navigation error to Register: " + e.getMessage());
            }
        });

        // --- Lógica Inicial ---
        // Verificar estado de autenticación al iniciar el fragmento.
        // Si el usuario ya está logueado, el observer de isAuthenticated lo detectará y navegará.
        authViewModel.checkAuthenticationState();

    }

    // Método llamado cuando la vista del fragmento es destruida.
    // Limpiamos referencias a las vistas para evitar posibles fugas de memoria.
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Poner las referencias UI a null
        editTextEmail = null;
        editTextPassword = null;
        buttonLogin = null;
        buttonRegister = null;
        progressBar = null;
        // La referencia al NavController también puede limpiarse
        navController = null;

        // NOTA: Los observers LiveData se limpian automáticamente cuando el LifecycleOwner
        // (getViewLifecycleOwner()) se destruye (que ocurre cuando onDestroyView se llama).
        // No necesitas remover los observers manualmente si usas getViewLifecycleOwner().
    }
}