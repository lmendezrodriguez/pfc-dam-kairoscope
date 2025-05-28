// android_app/app/src/main/java/com/lmr/kairoscope/data/repository/AuthRepository.java
// (Asegúrate de que el nombre del paquete arriba coincide con la ubicación real del archivo)

package com.lmr.kairoscope.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.lmr.kairoscope.data.model.UserProfile;
import com.lmr.kairoscope.data.model.AuthResult;
import com.lmr.kairoscope.util.NetworkUtils;

// Clase responsable de interactuar con Firebase Authentication
public class AuthRepository {
    private final Context context;
    private final FirebaseAuth firebaseAuth;

    // LiveData para comunicar el resultado de las operaciones de autenticación (login/registro)
    // El ViewModel observará esto
    private final MutableLiveData<AuthResult> authResultLiveData = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> currentUserProfileLiveData = new MutableLiveData<>();
    // LiveData para comunicar si el usuario está autenticado actualmente
    // El ViewModel (y quizás la Activity/otro Fragment) observará esto para manejar flujos
    private final MutableLiveData<Boolean> isAuthenticatedLiveData = new MutableLiveData<>();

    // Constructor. Aquí obtenemos la instancia de FirebaseAuth.
    public AuthRepository(Context context) {
        this.context = context;
        this.firebaseAuth = FirebaseAuth.getInstance();
        // Opcional: Verificar el estado de autenticación al crear el repositorio
        checkAuthenticationState();
    }

    public LiveData<UserProfile> getCurrentUserProfileLiveData() {
        return currentUserProfileLiveData;
    }

    // Exponer los LiveData para que el ViewModel pueda observarlos (solo lectura)
    public LiveData<AuthResult> getAuthResultLiveData() {
        return authResultLiveData;
    }

    public LiveData<Boolean> isAuthenticatedLiveData() {
        return isAuthenticatedLiveData;
    }

    // Método para iniciar sesión con email y contraseña
    public void login(String email, String password) {
            // Verificar conexión AÑADIR
            if (!NetworkUtils.isNetworkAvailable(context)) {
                authResultLiveData.postValue(new AuthResult(false, "Sin conexión a internet"));
                isAuthenticatedLiveData.postValue(false);
                return;
            }
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Mapear FirebaseUser a tu UserProfile
                            UserProfile userProfile = new UserProfile(firebaseUser.getUid(), firebaseUser.getDisplayName());
                            currentUserProfileLiveData.postValue(userProfile);
                        } else {
                            currentUserProfileLiveData.postValue(null); // Opcional: Si por alguna razón es null
                        }
                        authResultLiveData.postValue(new AuthResult(true, null));
                        isAuthenticatedLiveData.postValue(true);
                    } else {
                        // AQUÍ ESTÁ LA CORRECCIÓN: Capturar el mensaje de error de Firebase
                        String errorMessage = "Error de autenticación";
                        if (task.getException() != null) {
                            // Registrar el error en el logcat para depuración
                            Log.e("AuthRepository", "Login error: " + task.getException().getMessage());
                            errorMessage = task.getException().getMessage();
                        }
                        // Enviar el mensaje de error al AuthResult
                        authResultLiveData.postValue(new AuthResult(false, errorMessage));
                        currentUserProfileLiveData.postValue(null); // Usuario no autenticado, perfil es null
                        isAuthenticatedLiveData.postValue(false);
                    }
                });
    }

    // Método para registrar un nuevo usuario con email y contraseña
    public void register(String email, String password) {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            authResultLiveData.postValue(new AuthResult(false, "Sin conexión a internet"));
            currentUserProfileLiveData.postValue(null);
            isAuthenticatedLiveData.postValue(false);
            return;
        }
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Mapear FirebaseUser a tu UserProfile
                            UserProfile userProfile = new UserProfile(firebaseUser.getUid(), firebaseUser.getDisplayName());
                            currentUserProfileLiveData.postValue(userProfile);
                        } else {
                            currentUserProfileLiveData.postValue(null);
                        }
                        authResultLiveData.postValue(new AuthResult(true, null));
                        isAuthenticatedLiveData.postValue(true); // Usuario logueado después de registrar
                    } else {
                        // AQUÍ ESTÁ LA CORRECCIÓN: Capturar el mensaje de error de Firebase
                        String errorMessage = "Error de registro";
                        if (task.getException() != null) {
                            // Registrar el error en el logcat para depuración
                            Log.e("AuthRepository", "Register error: " + task.getException().getMessage());
                            errorMessage = task.getException().getMessage();
                        }
                        // Enviar el mensaje de error al AuthResult
                        authResultLiveData.postValue(new AuthResult(false, errorMessage));
                        currentUserProfileLiveData.postValue(null); // Usuario no autenticado si falla
                        isAuthenticatedLiveData.postValue(false);
                    }
                });
    }

    // Método para obtener el usuario actualmente logueado
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    // Método para verificar el estado de autenticación y actualizar el LiveData
    public void checkAuthenticationState() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        boolean isAuthenticated = firebaseUser != null;
        isAuthenticatedLiveData.postValue(isAuthenticated);

        if (isAuthenticated) {
            // Mapear FirebaseUser a tu UserProfile si hay usuario logueado
            UserProfile userProfile = new UserProfile(firebaseUser.getUid(), firebaseUser.getDisplayName());
            currentUserProfileLiveData.postValue(userProfile);
        } else {
            currentUserProfileLiveData.postValue(null); // No hay usuario, perfil es null
        }
    }

    // Método para cerrar sesión
    public void logout() {
        firebaseAuth.signOut();
        isAuthenticatedLiveData.postValue(false); // El usuario ya no está autenticado
        currentUserProfileLiveData.postValue(null); // El perfil del usuario ya no está disponible
    }
}