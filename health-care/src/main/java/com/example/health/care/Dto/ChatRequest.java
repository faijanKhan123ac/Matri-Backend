package com.example.health.care.Dto;

public class ChatRequest {
    private String message;
    private Long userId; // Optional — kon patient pooch raha hai track karne ke liye

    // Getters and Setters

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
