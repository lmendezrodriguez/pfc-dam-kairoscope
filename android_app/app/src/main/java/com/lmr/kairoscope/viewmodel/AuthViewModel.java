package com.lmr.kairoscope.viewmodel;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.lmr.kairoscope.data.model.AuthResult; // Importa tu clase AuthResult
import com.lmr.kairoscope.data.model.UserProfile; // Importa tu clase UserProfile
import com.lmr.kairoscope.data.repository.AuthRepository; // Importa tu Repository

// ViewModel para manejar la lógica de autenticación
public class AuthViewModel extends ViewModel {

    private static final String TAG = "AuthViewModel";
    private final AuthRepository authRepository;

    // LiveData que la UI observará
    // Estado de carga (para ProgressBar)
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    // Estado de autenticación (si el usuario está logueado o no)
    // Este LiveData viene directamente del Repository
    public LiveData<Boolean> isAuthenticated() { return authRepository.isAuthenticatedLiveData(); }

    // Perfil del usuario autenticado (si existe)
    // Este LiveData también viene directamente del Repository
    public LiveData<UserProfile> getCurrentUserProfile() { return authRepository.getCurrentUserProfileLiveData(); }

    // Resultado de la última operación de autenticación (para manejo detallado si se necesita más que solo el estado general)
    // También viene del Repository
    public LiveData<AuthResult> getAuthResult() { return authRepository.getAuthResultLiveData(); }

    // *** NUEVO: LiveData para mensajes de UI (éxito o error) ***
    // El ViewModel generará mensajes simples que la UI mostrará (ej: en un Snackbar)
    private final MutableLiveData<String> message = new MutableLiveData<>();
    public LiveData<String> getMessage() { return message; }


    // Constructor. Recibe el Repository.
    public AuthViewModel(AuthRepository authRepository) {
        this.authRepository = authRepository;

        // Opcional pero útil: Observar el AuthResult del Repository para generar mensajes
        // y actualizar el estado de carga aquí mismo en el ViewModel.
        // Esto centraliza el manejo de los resultados del Repository.
        this.authRepository.getAuthResultLiveData().observeForever(result -> {
            // NOTA: observeForever puede causar fugas de memoria si no se remueve.
            // En un Fragmento, usarías observe(getViewLifecycleOwner(), ...).
            // Aquí en el VM, es para procesar eventos del Repo que vive más tiempo.
            // Un enfoque más robusto en VMs es usar Flows de Kotlin o SingleLiveEvent.
            // Pero para empezar, esto funciona. Asegúrate de gestionar isLoading.

            if (result != null) {
                // Importante: Primero detener la carga antes de procesar el resultado
                Log.d(TAG, "AuthResult received: Success=" + result.isSuccess() + ", Error=" + result.getErrorMessage());
                isLoading.postValue(false); // La operación terminó, detener la carga PRIMERO

                // Procesar el resultado y postear un mensaje para la UI
                if (result.isSuccess()) {
                    message.postValue("Operación exitosa."); // Mensaje genérico o específico
                } else {
                    // Asegurarse de que el mensaje de error nunca sea null
                    String errorMsg = result.getErrorMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "Error de autenticación desconocido";
                    }
                    message.postValue("Error: " + errorMsg);
                }
            }
        });
    }

    // Método público para iniciar sesión (llamado por el Fragmento de Login)
    public void login(String email, String password) {
        isLoading.setValue(true); // Indicar que la operación está en curso
        // Llamar al Repository para realizar el login.
        // El Repository gestionará la operación asíncrona y actualizará sus LiveData.
        authRepository.login(email, password);
        // El observer de authResultLiveData (definido arriba) capturará el resultado y actualizará isLoading y message.
    }

    // Método público para registrar (llamado por el Fragmento de Registro)
    public void register(String email, String password) {
        isLoading.setValue(true); // Indicar que la operación está en curso
        authRepository.register(email, password);
        // El observer de authResultLiveData capturará el resultado y actualizará isLoading y message.
    }

    // Método público para verificar el estado de autenticación al inicio (llamado por el Fragmento de Login/Splash)
    public void checkAuthenticationState() {
        authRepository.checkAuthenticationState();
        // El Repository actualizará isAuthenticatedLiveData y currentUserProfileLiveData.
    }

    // Método público para cerrar sesión
    public void logout() {
        isLoading.setValue(true); // Opcional, puedes mostrar carga al cerrar sesión
        authRepository.logout();
        // Repository actualizará LiveData. isAuthenticaded pasará a false, profile a null.
        isLoading.postValue(false); // Cerrar sesión es generalmente rápido
    }

    // Método para "consumir" el mensaje una vez mostrado por la UI
    // Útil para evitar que el mensaje reaparezca en recreaciones de vista
    public void clearMessage() {
        message.setValue(null);
    }

    // Asegurarse de limpiar el observador cuando el ViewModel se destruya
    @Override
    protected void onCleared() {
        super.onCleared();
        // Aquí podríamos remover el observeForever si fuera necesario
        // (pero en este caso específico no tenemos una referencia al observer)
    }

    // *** Factory para crear el ViewModel ***
    // Necesitamos un Factory porque el ViewModel tiene un constructor con parámetros (AuthRepository)
    public static class Factory extends ViewModelProvider.NewInstanceFactory {
        private final AuthRepository repository;

        public Factory(AuthRepository repository) {
            this.repository = repository;
        }

        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(AuthViewModel.class)) {
                // Si la clase solicitada es AuthViewModel, la creamos pasando el repository
                return (T) new AuthViewModel(repository);
            }
            // Si no es AuthViewModel, lanzamos una excepción (comportamiento estándar)
            throw new IllegalArgumentException("Unknown ViewModel class: " + modelClass.getName());
        }
    }
}