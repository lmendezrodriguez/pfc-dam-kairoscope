package com.lmr.kairoscope.data.model;

public class AuthResult {
    private boolean success;
    private String errorMessage;

    public AuthResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
}
