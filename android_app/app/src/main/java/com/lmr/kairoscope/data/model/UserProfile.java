package com.lmr.kairoscope.data.model;

/**
 * Modelo que representa el perfil de usuario en la aplicación.
 * Vincula los datos de Firebase Auth con la información local del usuario.
 */
public class UserProfile {
    private String firebaseUid;
    private String displayName;
    private String email;

    public UserProfile(String firebaseUid, String displayName, String email) {
        this.firebaseUid = firebaseUid;
        this.displayName = displayName;
        this.email = email;
    }

    // UID único de Firebase para identificar al usuario
    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}