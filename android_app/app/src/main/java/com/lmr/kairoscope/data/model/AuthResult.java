package com.lmr.kairoscope.data.model;

/***
 * Clase para manejar el resultado de la autenticación.
 */
public class AuthResult {
    private boolean success;
    private String errorMessage;

    // Constructor
    public AuthResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    // Métodos getter
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
}
