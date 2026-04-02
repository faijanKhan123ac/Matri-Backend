package com.example.health.care.Controller;

import com.example.health.care.Service.GamificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/gamification")
@CrossOrigin(origins = "*")
public class GamificationController {

    @Autowired
    private GamificationService gamificationService;

    // GET /api/gamification/stats/{userId}
    @GetMapping("/stats/{userId}")
    public ResponseEntity<?> getStats(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(gamificationService.getUserStats(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/gamification/points/{userId}
    @PostMapping("/points/{userId}")
    public ResponseEntity<?> addPoints(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> body) {
        try {
            int points    = Integer.parseInt(body.get("points").toString());
            String reason = body.getOrDefault("reason", "activity").toString();
            gamificationService.addPoints(userId, points, reason);
            return ResponseEntity.ok(gamificationService.getUserStats(userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/gamification/leaderboard
    @GetMapping("/leaderboard")
    public ResponseEntity<?> getLeaderboard() {
        try {
            return ResponseEntity.ok(gamificationService.getLeaderboard());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}