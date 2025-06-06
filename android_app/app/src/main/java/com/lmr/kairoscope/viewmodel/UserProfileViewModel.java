package com.lmr.kairoscope.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.lmr.kairoscope.data.model.UserProfile;
import com.lmr.kairoscope.data.repository.AuthRepository;

/**
 * ViewModel que gestiona el perfil del usuario y operaciones de cuenta.
 * Permite actualizar información personal, cambiar contraseña y cerrar sesión.
 */
public class UserProfileViewModel extends ViewModel {

    private final AuthRepository authRepository;

    // Estados de carga para diferentes operaciones
    private final MutableLiveData<Boolean> isUpdatingProfile = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isUpdatingPassword = new MutableLiveData<>(false);

    // Mensajes de feedback
    private final MutableLiveData<String> message = new MutableLiveData<>();

    /**
     * Constructor que inicializa el ViewModel con el Repository de autenticación.
     */
    public UserProfileViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    // Getters para LiveData
    public LiveData<UserProfile> getCurrentUserProfile() { return authRepository.getCurrentUserProfileLiveData(); }
    public LiveData<Boolean> isAuthenticated() { return authRepository.isAuthenticatedLiveData(); }
    public LiveData<Boolean> getIsUpdatingProfile() { return isUpdatingProfile; }
    public LiveData<Boolean> getIsUpdatingPassword() { return isUpdatingPassword; }
    public LiveData<String> getMessage() { return message; }

    /**
     * Actualiza el nombre de usuario en el perfil.
     */
    public void updateProfile(String newDisplayName) {
        isUpdatingProfile.setValue(true);
        authRepository.updateUserProfile(newDisplayName);
        isUpdatingProfile.setValue(false);
        message.setValue("Perfil actualizado correctamente");
    }

    /**
     * Cambia la contraseña del usuario tras validar confirmación.
     */
    public void updatePassword(String currentPassword, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            message.setValue("Las contraseñas no coinciden");
            return;
        }

        isUpdatingPassword.setValue(true);
        // Callback para manejar resultado asíncrono de cambio de contraseña
        authRepository.updatePassword(currentPassword, newPassword, (success, error) -> {
            isUpdatingPassword.postValue(false);
            message.postValue(success ? "Contraseña actualizada correctamente" : "Error: " + error);
        });
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    public void logout() {
        authRepository.logout();
    }

    /**
     * Limpia el mensaje actual para evitar que se muestre nuevamente.
     */
    public void clearMessage() {
        message.setValue(null);
    }

    /**
     * Factory para crear instancias del ViewModel con dependencias.
     */
    public static class Factory extends ViewModelProvider.NewInstanceFactory {
        private final AuthRepository repository;

        public Factory(AuthRepository repository) {
            this.repository = repository;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(UserProfileViewModel.class)) {
                return (T) new UserProfileViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}