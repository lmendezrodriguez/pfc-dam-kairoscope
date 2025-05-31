package com.lmr.kairoscope.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.lmr.kairoscope.data.model.UserProfile;
import com.lmr.kairoscope.data.repository.AuthRepository;

public class UserProfileViewModel extends ViewModel {

    private final AuthRepository authRepository;

    // Estados de carga
    private final MutableLiveData<Boolean> isUpdatingProfile = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isUpdatingPassword = new MutableLiveData<>(false);

    // Mensajes
    private final MutableLiveData<String> message = new MutableLiveData<>();

    public UserProfileViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;
    }

    // Getters
    public LiveData<UserProfile> getCurrentUserProfile() { return authRepository.getCurrentUserProfileLiveData(); }
    public LiveData<Boolean> isAuthenticated() { return authRepository.isAuthenticatedLiveData(); }
    public LiveData<Boolean> getIsUpdatingProfile() { return isUpdatingProfile; }
    public LiveData<Boolean> getIsUpdatingPassword() { return isUpdatingPassword; }
    public LiveData<String> getMessage() { return message; }

    public void updateProfile(String newDisplayName) {
        isUpdatingProfile.setValue(true);
        authRepository.updateUserProfile(newDisplayName);
        isUpdatingProfile.setValue(false);
        message.setValue("Perfil actualizado correctamente");
    }

    public void updatePassword(String currentPassword, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            message.setValue("Las contraseñas no coinciden");
            return;
        }

        isUpdatingPassword.setValue(true);
        authRepository.updatePassword(currentPassword, newPassword, (success, error) -> {
            isUpdatingPassword.postValue(false);
            message.postValue(success ? "Contraseña actualizada correctamente" : "Error: " + error);
        });
    }

    public void logout() {
        authRepository.logout();
    }

    public void clearMessage() {
        message.setValue(null);
    }

    // Factory
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