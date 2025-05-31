// view/fragment/UserProfileFragment.java
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

    // ViewModel
    private UserProfileViewModel viewModel;

    // Navigation
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

    private void setupViewModel() {
        AuthRepository authRepository = new AuthRepository(requireContext());
        viewModel = new ViewModelProvider(this, new UserProfileViewModel.Factory(authRepository))
                .get(UserProfileViewModel.class);
    }

    private void setupObservers() {
        // Observar perfil del usuario
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        viewModel.getCurrentUserProfile().observe(getViewLifecycleOwner(), userProfile -> {
            if (userProfile != null) {
                textViewUserName.setText(userProfile.getDisplayName() != null ?
                        userProfile.getDisplayName() : "Usuario");
                textViewUserEmail.setText(userProfile.getEmail() != null ?
                        userProfile.getEmail() : "email@ejemplo.com");

                // Llenar campos editables
                editTextName.setText(userProfile.getDisplayName());
                editTextEmail.setText(userProfile.getEmail());
            }
        });

        // Observar estado de autenticación
        viewModel.isAuthenticated().observe(getViewLifecycleOwner(), isAuthenticated -> {
            if (isAuthenticated != null && !isAuthenticated) {
                // Usuario deslogueado, navegar a login
                try {
                    navController.navigate(R.id.action_userProfileFragment_to_loginFragment);
                } catch (Exception e) {
                    // Manejo de error de navegación
                }
            }
        });

        // Observar estados de carga
        viewModel.getIsUpdatingProfile().observe(getViewLifecycleOwner(), isUpdating -> {
            buttonSaveChanges.setEnabled(!isUpdating);
            if (isUpdating) {
                buttonSaveChanges.setText("Guardando...");
            } else {
                buttonSaveChanges.setText("Guardar Cambios");
            }
        });

        viewModel.getIsUpdatingPassword().observe(getViewLifecycleOwner(), isUpdating -> {
            buttonSaveChanges.setEnabled(!isUpdating);
        });

        // Observar mensajes
        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show();
                viewModel.clearMessage();

                // Limpiar campos de contraseña después de actualización exitosa
                if (message.contains("Contraseña actualizada")) {
                    clearPasswordFields();
                }
            }
        });
    }

    private void setupListeners() {
        // Botón guardar cambios
        buttonSaveChanges.setOnClickListener(v -> {
            saveChanges();
        });

        // Botón logout
        buttonLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Cerrar sesión")
                    .setMessage("¿Estás seguro de que quieres cerrar sesión?")
                    .setPositiveButton("Cerrar sesión", (dialog, which) -> {
                        viewModel.logout();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void saveChanges() {
        String newName = editTextName.getText().toString().trim();
        String currentPassword = editTextCurrentPassword.getText().toString().trim();
        String newPassword = editTextNewPassword.getText().toString().trim();
        String confirmPassword = editTextConfirmPassword.getText().toString().trim();

        // Validar si hay cambios en el perfil
        if (!newName.isEmpty()) {
            viewModel.updateProfile(newName);
        }

        // Validar si hay cambio de contraseña
        if (!currentPassword.isEmpty() && !newPassword.isEmpty()) {
            if (newPassword.length() < 6) {
                Snackbar.make(requireView(), "La contraseña debe tener al menos 6 caracteres",
                        Snackbar.LENGTH_SHORT).show();
                return;
            }
            viewModel.updatePassword(currentPassword, newPassword, confirmPassword);
        } else if (!currentPassword.isEmpty() || !newPassword.isEmpty() || !confirmPassword.isEmpty()) {
            Snackbar.make(requireView(), "Completa todos los campos de contraseña",
                    Snackbar.LENGTH_SHORT).show();
        }
    }

    private void clearPasswordFields() {
        editTextCurrentPassword.setText("");
        editTextNewPassword.setText("");
        editTextConfirmPassword.setText("");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiar referencias
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