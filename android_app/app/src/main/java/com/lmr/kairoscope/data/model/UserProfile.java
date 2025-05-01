package com.lmr.kairoscope.data.model;

public class UserProfile {
    private String firebaseUid;
    private String displayName; // Campo para el nombre del usuario

    // Constructor básico con UID y nombre
    public UserProfile(String firebaseUid, String displayName) {
        this.firebaseUid = firebaseUid;
        this.displayName = displayName;
    }

    // Getters
    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getDisplayName() {
        return displayName;
    }

    // Puedes añadir setters si la lógica de tu app lo requiere,
    // aunque a menudo los modelos de datos pasivos solo tienen getters.
}
