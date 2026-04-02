package com.example.health.care.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class ChatbotService {

    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient = WebClient.create();

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=";

    // =============================================
    // RAMESH — Dost jaisa AI Doctor
    // =============================================
    private static final String RAMESH_PROMPT =
            "Tu Ramesh hai — ek casual aur friendly male AI health dost. " +
                    "Teri baat karne ki style bilkul dost jaisi hai — 'yaar', 'bhai', 'arre', 'ghabra mat' use karta hai. " +
                    "Pehle sympathize karo, phir simple solution do, phir encourage karo. " +
                    "RULES:\n" +
                    "1. Sirf health related sawaalon ka jawab do\n" +
                    "2. Hinglish mein baat karo jab tak English mein na puchha jaaye\n" +
                    "3. Emergency pe bold mein likho: YAAR TURANT DOCTOR KE PAAS JAO!\n" +
                    "4. End mein hamesha: 'But yaar real doctor se zaroor milo!'\n" +
                    "5. Chhota aur clear jawab do — jaise dost phone pe bolta hai\n\n" +
                    "EXAMPLE:\n" +
                    "User: bukhar hai\n" +
                    "Ramesh: Arre yaar! 🤒 Bukhar hai toh paani zyada pi, rest kar. " +
                    "Paracetamol 500mg le sakta hai — safe hai. " +
                    "Agar 3 din se zyada ho ya 103F se zyada ho toh doctor ke paas jao bhai! " +
                    "But yaar real doctor se zaroor milo!\n\n" +
                    "User ka sawaal: ";

    private static final String SYMPTOM_PROMPT =
            "Tu Ramesh hai — ek friendly AI health dost jo symptoms sun ke help karta hai. " +
                    "Symptoms sun ke is style mein Hinglish mein jawab do:\n\n" +
                    "Yaar kya ho raha hai: [simple explanation]\n\n" +
                    "Ghar pe karo: [2-3 easy remedies]\n\n" +
                    "Safe dawai: [common OTC medicine]\n\n" +
                    "Doctor ke paas kab: [warning signs]\n\n" +
                    "Ramesh ki advice: [encouraging dost jaili closing line]\n\n" +
                    "Hamesha end mein: 'But bhai real doctor se zaroor milna!'\n\n" +
                    "Symptoms: ";

    public String getChatbotResponse(String message) {
        return callGemini(RAMESH_PROMPT + message);
    }

    public String analyzeSymptoms(String symptoms) {
        return callGemini(SYMPTOM_PROMPT + symptoms);
    }

    private String callGemini(String fullPrompt) {
        String url = GEMINI_URL + apiKey;

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", fullPrompt)
                        ))
                )
        );

        try {
            Map response = webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null)
                return "Yaar AI se response nahi aaya. Dobara try karo!";

            List candidates = (List) response.get("candidates");
            if (candidates == null || candidates.isEmpty())
                return "Yaar koi response nahi mila. Sawaal dobara poochho!";

            Map candidate = (Map) candidates.get(0);
            Map content   = (Map) candidate.get("content");
            List parts    = (List) content.get("parts");
            Map part      = (Map) parts.get(0);
            return (String) part.get("text");

        } catch (WebClientResponseException e) {
            return "AI service abhi nahi hai (Error: " + e.getStatusCode() + "). Baad mein try karo yaar!";
        } catch (Exception e) {
            return "Kuch error aa gaya yaar. Please dobara try karo!";
        }
    }
}