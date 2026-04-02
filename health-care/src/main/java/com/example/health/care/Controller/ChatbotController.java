package com.example.health.care.Controller;

import com.example.health.care.Dto.ChatRequest;
import com.example.health.care.Service.ChatbotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    // POST /api/chat
    @PostMapping
    public ResponseEntity<?> getChatbotResponse(@RequestBody ChatRequest chatRequest) {
        String response = chatbotService.getChatbotResponse(chatRequest.getMessage());
        // JSON mein wrap karo — frontend ka data.response kaam karega
        return ResponseEntity.ok(Map.of("response", response));
    }

    // POST /api/chat/symptoms
    @PostMapping("/symptoms")
    public ResponseEntity<?> analyzeSymptoms(@RequestBody Map<String, String> body) {
        String symptoms = body.getOrDefault("symptoms", "");
        String response = chatbotService.analyzeSymptoms(symptoms);
        return ResponseEntity.ok(Map.of("response", response));
    }
}