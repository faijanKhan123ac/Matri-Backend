package com.example.health.care.Dto;

public class ChatResponse {
    private String response;
    private boolean success;

    public ChatResponse(String response) {
        this.response = response;
        this.success = true;
    }

    public ChatResponse(String response, boolean success) {
        this.response = response;
        this.success = success;
    }

    // Getters and Setters

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
