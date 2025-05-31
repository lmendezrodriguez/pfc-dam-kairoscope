package com.lmr.kairoscope.data.model;

public class UserProfile {
    private String firebaseUid;
    private String displayName;
    private String email;

    public UserProfile(String firebaseUid, String displayName, String email) {
        this.firebaseUid = firebaseUid;
        this.displayName = displayName;
        this.email = email;
    }

    // Getters y setters
    public String getFirebaseUid() { return firebaseUid; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setEmail(String email) { this.email = email; }
}