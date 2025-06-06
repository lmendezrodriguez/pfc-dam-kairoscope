package com.lmr.kairoscope.view.fragment;

import android.os.Bundle;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lmr.kairoscope.R;
import com.lmr.kairoscope.data.repository.AuthRepository;
import com.lmr.kairoscope.viewmodel.UserProfileViewModel;

import java.util.Objects;

/**
 * Fragment para gestionar el perfil del usuario.
 * Permite editar nombre, cambiar contraseña y cerrar sesión.
 */
public class UserProfileFragment extends Fragment {

    private static final String TAG = "UserProfileFragment";

    // UI Elements
    private android.widget.TextView textViewUserName;
    private android.widget.TextView textViewUserEmail;
    private TextInputEditText editTextName;
    private TextInputEditText editTextEmail;
    private TextInputEditText editTextCurrentPassword;
    private TextInputEditText editTextNewPassword;
    private TextInputEditText editTextConfirmPassword;
    private MaterialButton buttonSaveChanges;
    private MaterialButton buttonLogout;

    // ViewModel y navegación
    private UserProfileViewModel viewModel;
    private NavController navController;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupViewModel();
        setupObservers();
        setupListeners();
    }

    /**
     * Inicializa las referencias a los elementos de la UI.
     */
    private void initViews(View view) {
        textViewUserName = view.findViewById(R.id.textViewUserName);
        textViewUserEmail = view.findViewById(R.id.textViewUserEmail);
        editTextName = view.findViewById(R.id.editTextName);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextCurrentPassword = view.findViewById(R.id.editTextCurrentPassword);
        editTextNewPassword = view.findViewById(R.id.editTextNewPassword);
        editTextConfirmPassword = view.findViewById(R.id.editTextConfirmPassword);
        buttonSaveChanges = view.findViewById(R.id.buttonSaveChanges);
        buttonLogout = view.findViewById(R.id.buttonLogout);

        navController = NavHostFragment.findNavController(this);
    }

    /**
     * Configura el ViewModel con sus dependencias.
     */
    private void setupViewModel() {
        AuthRepository authRepository = new AuthRepository(requireContext());
        viewModel = new ViewModelProvider(this, new UserProfileViewModel.Factory(authRepository))
                .get(UserProfileViewModel.class);
    }

    /**
     * Configura los observadores para actualizar la UI según los cambios del ViewModel.
     */
    private void setupObservers() {
        // Observar perfil del usuario y poblar campos
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        viewModel.getCurrentUserProfile().observe(getViewLifecycleOwner(), userProfile -> {
            if (userProfile != null) {
                // Mostrar información actual
                textViewUserName.setText(userProfile.getDisplayName() != null ?
                        userProfile.getDisplayName() : "Usuario");
                textViewUserEmail.setText(userProfile.getEmail() != null ?
                        userProfile.getEmail() : "email@ejemplo.com");

                // Poblar campos editables
                if (editTextName != null) {
                    editTextName.setText(userProfile.getDisplayName());
                }
                if (editTextEmail != null) {
                    editTextEmail.setText(userProfile.getEmail());
                }
            }
        });

        // Observar estado de autenticación para navegación post-logout
        viewModel.isAuthenticated().observe(getViewLifecycleOwner(), isAuthenticated -> {
            if (isAuthenticated != null && !isAuthenticated) {
                try {
                    navController.navigate(R.id.action_userProfileFragment_to_loginFragment);
                } catch (Exception e) {
                    // Silenciar error de navegación si el fragment ya no está activo
                }
            }
        });

        // Observar estados de carga para feedback visual
        viewModel.getIsUpdatingProfile().observe(getViewLifecycleOwner(), isUpdating -> {
            buttonSaveChanges.setEnabled(!isUpdating);
            buttonSaveChanges.setText(isUpdating ? "Guardando..." : "Guardar Cambios");
        });

        viewModel.getIsUpdatingPassword().observe(getViewLifecycleOwner(), isUpdating -> {
            buttonSaveChanges.setEnabled(!isUpdating);
        });

        // Observar mensajes y limpiar campos tras operaciones exitosas
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                viewModel.clearMessage();

                // Limpiar campos de contraseña tras cambio exitoso
                if (message.contains("Contraseña actualizada")) {
                    clearPasswordFields();
                }
            }
        });
    }

    /**
     * Configura los listeners de los botones.
     */
    private void setupListeners() {
        // Botón guardar cambios con validación
        buttonSaveChanges.setOnClickListener(v -> saveChanges());

        // Botón logout con confirmación
        buttonLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Cerrar sesión")
                    .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                    .setPositiveButton("Cerrar sesión", (dialog, which) -> viewModel.logout())
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    /**
     * Valida y guarda los cambios del perfil.
     */
    private void saveChanges() {
        String newName = Objects.requireNonNull(editTextName.getText()).toString().trim();
        String currentPassword = Objects.requireNonNull(editTextCurrentPassword.getText()).toString().trim();
        String newPassword = Objects.requireNonNull(editTextNewPassword.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(editTextConfirmPassword.getText()).toString().trim();

        // Actualizar nombre si cambió
        if (!newName.isEmpty()) {
            viewModel.updateProfile(newName);
        }

        // Cambiar contraseña si todos los campos están completos
        if (!currentPassword.isEmpty() && !newPassword.isEmpty() && !confirmPassword.isEmpty()) {
            if (newPassword.length() < 6) {
                Snackbar.make(requireView(), "La contraseña debe tener al menos 6 caracteres",
                        Snackbar.LENGTH_SHORT).show();
                viewModel.clearMessage();
                return;
            }
            viewModel.updatePassword(currentPassword, newPassword, confirmPassword);
        }
    }

    /**
     * Limpia los campos de contraseña tras una operación exitosa.
     */
    private void clearPasswordFields() {
        editTextCurrentPassword.setText("");
        editTextNewPassword.setText("");
        editTextConfirmPassword.setText("");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Prevenir memory leaks limpiando referencias
        textViewUserName = null;
        textViewUserEmail = null;
        editTextName = null;
        editTextEmail = null;
        editTextCurrentPassword = null;
        editTextNewPassword = null;
        editTextConfirmPassword = null;
        buttonSaveChanges = null;
        buttonLogout = null;
    }
}