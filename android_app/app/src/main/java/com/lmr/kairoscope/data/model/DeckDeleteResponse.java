package com.lmr.kairoscope.data.model;

public class DeckDeleteResponse {
    private String status;
    private String message;

    public DeckDeleteResponse() {
    }

    public DeckDeleteResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return "success".equals(status);
    }
}
