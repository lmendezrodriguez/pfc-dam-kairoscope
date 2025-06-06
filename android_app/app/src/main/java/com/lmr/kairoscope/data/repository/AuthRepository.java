package com.lmr.kairoscope.data.repository;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import com.lmr.kairoscope.data.model.UserProfile;
import com.lmr.kairoscope.data.model.AuthResult;
import com.lmr.kairoscope.util.NetworkUtils;

/**
 * Repositorio responsable de gestionar la autenticación con Firebase.
 * Proporciona métodos para login, registro, actualización de perfil y gestión de sesiones.
 */
public class AuthRepository {
    private final Context context;
    private final FirebaseAuth firebaseAuth;

    // LiveData para comunicar estados de autenticación al ViewModel
    private final MutableLiveData<AuthResult> authResultLiveData = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> currentUserProfileLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAuthenticatedLiveData = new MutableLiveData<>();

    public AuthRepository(Context context) {
        this.context = context;
        this.firebaseAuth = FirebaseAuth.getInstance();
        checkAuthenticationState();
    }

    public LiveData<UserProfile> getCurrentUserProfileLiveData() {
        return currentUserProfileLiveData;
    }

    public LiveData<AuthResult> getAuthResultLiveData() {
        return authResultLiveData;
    }

    public LiveData<Boolean> isAuthenticatedLiveData() {
        return isAuthenticatedLiveData;
    }

    /**
     * Inicia sesión con email y contraseña verificando conectividad.
     */
    public void login(String email, String password) {
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
                            UserProfile userProfile = new UserProfile(
                                    firebaseUser.getUid(),
                                    firebaseUser.getDisplayName(),
                                    firebaseUser.getEmail()
                            );
                            currentUserProfileLiveData.postValue(userProfile);
                        } else {
                            currentUserProfileLiveData.postValue(null);
                        }
                        authResultLiveData.postValue(new AuthResult(true, null));
                        isAuthenticatedLiveData.postValue(true);
                    } else {
                        // Capturar mensaje de error específico de Firebase
                        String errorMessage = "Error de autenticación";
                        if (task.getException() != null) {
                            Log.e("AuthRepository", "Login error: " + task.getException().getMessage());
                            errorMessage = task.getException().getMessage();
                        }
                        authResultLiveData.postValue(new AuthResult(false, errorMessage));
                        currentUserProfileLiveData.postValue(null);
                        isAuthenticatedLiveData.postValue(false);
                    }
                });
    }

    /**
     * Registra un nuevo usuario y actualiza su perfil con el nombre proporcionado.
     */
    public void register(String email, String password, String displayName) {
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
                            // Actualizar perfil con el nombre de usuario
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(displayName)
                                    .build();

                            firebaseUser.updateProfile(profileUpdates)
                                    .addOnCompleteListener(updateTask -> {
                                        UserProfile userProfile = new UserProfile(
                                                firebaseUser.getUid(),
                                                displayName,
                                                firebaseUser.getEmail()
                                        );
                                        currentUserProfileLiveData.postValue(userProfile);
                                        authResultLiveData.postValue(new AuthResult(true, null));
                                        isAuthenticatedLiveData.postValue(true);
                                    });
                        }
                    } else {
                        String errorMessage = "Error de registro";
                        if (task.getException() != null) {
                            Log.e("AuthRepository", "Register error: " + task.getException().getMessage());
                            errorMessage = task.getException().getMessage();
                        }
                        authResultLiveData.postValue(new AuthResult(false, errorMessage));
                        currentUserProfileLiveData.postValue(null);
                        isAuthenticatedLiveData.postValue(false);
                    }
                });
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Verifica el estado actual de autenticación y actualiza los LiveData.
     */
    public void checkAuthenticationState() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        boolean isAuthenticated = firebaseUser != null;
        isAuthenticatedLiveData.postValue(isAuthenticated);

        if (isAuthenticated) {
            UserProfile userProfile = new UserProfile(
                    firebaseUser.getUid(),
                    firebaseUser.getDisplayName(),
                    firebaseUser.getEmail()
            );
            currentUserProfileLiveData.postValue(userProfile);
        } else {
            currentUserProfileLiveData.postValue(null);
        }
    }

    /**
     * Cierra la sesión del usuario actual.
     */
    public void logout() {
        firebaseAuth.signOut();
        isAuthenticatedLiveData.postValue(false);
        currentUserProfileLiveData.postValue(null);
    }

    /**
     * Actualiza el nombre de usuario en Firebase y en el LiveData local.
     */
    public void updateUserProfile(String newDisplayName) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(newDisplayName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            UserProfile updated = new UserProfile(user.getUid(), newDisplayName, user.getEmail());
                            currentUserProfileLiveData.postValue(updated);
                        }
                    });
        }
    }

    /**
     * Actualiza la contraseña del usuario requiriendo re-autenticación.
     */
    public void updatePassword(String currentPassword, String newPassword, PasswordUpdateCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            // Re-autenticar antes de cambiar contraseña
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(updateTask -> {
                                        callback.onResult(updateTask.isSuccessful(),
                                                updateTask.isSuccessful() ? null : updateTask.getException().getMessage());
                                    });
                        } else {
                            callback.onResult(false, "Contraseña actual incorrecta");
                        }
                    });
        }
    }

    /**
     * Interface para callbacks de actualización de contraseña.
     */
    public interface PasswordUpdateCallback {
        void onResult(boolean success, String error);
    }
}