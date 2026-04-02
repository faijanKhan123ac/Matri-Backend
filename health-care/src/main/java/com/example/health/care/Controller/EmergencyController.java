package com.example.health.care.Controller;

import com.example.health.care.Entity.FamilyMember;
import com.example.health.care.Service.EmergencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emergency")
@CrossOrigin(origins = "*")
public class EmergencyController {

    @Autowired
    private EmergencyService emergencyService;

    // POST /api/emergency/family — family member add karo
    @PostMapping("/family")
    public ResponseEntity<?> addFamilyMember(@RequestBody FamilyMember member) {
        try {
            FamilyMember saved = emergencyService.addFamilyMember(member);
            return ResponseEntity.ok(Map.of(
                    "message", "Family member add ho gaya!",
                    "id", saved.getId(),
                    "name", saved.getName()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/emergency/family/{userId} — sabhi family members lo
    @GetMapping("/family/{userId}")
    public ResponseEntity<?> getFamilyMembers(@PathVariable Long userId) {
        try {
            List<FamilyMember> members = emergencyService.getFamilyMembers(userId);
            return ResponseEntity.ok(members);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/emergency/family/{memberId}?userId=
    @DeleteMapping("/family/{memberId}")
    public ResponseEntity<?> deleteFamilyMember(@PathVariable Long memberId,
                                                @RequestParam Long userId) {
        try {
            emergencyService.deleteFamilyMember(memberId, userId);
            return ResponseEntity.ok(Map.of("message", "Family member remove ho gaya."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/emergency/alert — SOS button
    @PostMapping("/alert")
    public ResponseEntity<?> sendAlert(@RequestBody Map<String, Object> body) {
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            Double latitude  = body.get("latitude")  != null ? Double.valueOf(body.get("latitude").toString())  : null;
            Double longitude = body.get("longitude") != null ? Double.valueOf(body.get("longitude").toString()) : null;

            String result = emergencyService.sendEmergencyAlert(userId, latitude, longitude);
            return ResponseEntity.ok(Map.of("message", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}