package com.lmr.kairoscope.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.lmr.kairoscope.data.model.AuthResult;
import com.lmr.kairoscope.data.model.UserProfile;
import com.lmr.kairoscope.data.repository.AuthRepository;

/**
 * ViewModel que gestiona la lógica de autenticación y estado del usuario.
 * Centraliza las operaciones de login, registro y logout usando el Repository.
 */
public class AuthViewModel extends ViewModel {

    private static final String TAG = "AuthViewModel";
    private final AuthRepository authRepository;

    // Estado de carga para mostrar indicadores visuales
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    // Estados expuestos directamente del Repository
    public LiveData<Boolean> isAuthenticated() { return authRepository.isAuthenticatedLiveData(); }
    public LiveData<UserProfile> getCurrentUserProfile() { return authRepository.getCurrentUserProfileLiveData(); }
    public LiveData<AuthResult> getAuthResult() { return authRepository.getAuthResultLiveData(); }

    // Mensajes para mostrar feedback al usuario
    private final MutableLiveData<String> message = new MutableLiveData<>();
    public LiveData<String> getMessage() { return message; }

    /**
     * Constructor que inicializa el ViewModel con el Repository.
     * Configura observador para procesar resultados de autenticación.
     */
    public AuthViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;

        // Observar resultados del Repository para generar mensajes y gestionar carga
        this.authRepository.getAuthResultLiveData().observeForever(result -> {
            if (result != null) {
                Log.d(TAG, "AuthResult received: Success=" + result.isSuccess() + ", Error=" + result.getErrorMessage());

                // Detener carga al completarse la operación
                isLoading.postValue(false);

                // Generar mensaje apropiado para la UI
                if (result.isSuccess()) {
                    message.postValue("Operación exitosa.");
                } else {
                    String errorMsg = result.getErrorMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "Error de autenticación desconocido";
                    }
                    message.postValue("Error: " + errorMsg);
                }
            }
        });
    }

    /**
     * Inicia el proceso de login con email y contraseña.
     */
    public void login(String email, String password) {
        isLoading.setValue(true);
        authRepository.login(email, password);
    }

    /**
     * Inicia el proceso de registro con email, contraseña y nombre.
     */
    public void register(String email, String password, String displayName) {
        isLoading.setValue(true);
        authRepository.register(email, password, displayName);
    }

    /**
     * Verifica el estado actual de autenticación del usuario.
     */
    public void checkAuthenticationState() {
        authRepository.checkAuthenticationState();
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    public void logout() {
        isLoading.setValue(true);
        authRepository.logout();
        isLoading.postValue(false); // Logout es operación rápida
    }

    /**
     * Limpia el mensaje actual para evitar que se muestre nuevamente.
     */
    public void clearMessage() {
        message.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Los observers se limpian automáticamente al destruirse el ViewModel
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
            if (modelClass.isAssignableFrom(AuthViewModel.class)) {
                return (T) new AuthViewModel(repository);
            }
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}