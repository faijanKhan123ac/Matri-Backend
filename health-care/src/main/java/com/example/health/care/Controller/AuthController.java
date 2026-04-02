package com.example.health.care.Controller;

import com.example.health.care.Dto.LoginRequest;
import com.example.health.care.Entity.User;
import com.example.health.care.Service.GamificationService;
import com.example.health.care.Service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private GamificationService gamificationService;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody User user) {
        if (user.getEmail() == null || user.getEmail().trim().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Email zaroori hai."));
        if (user.getPassword() == null || user.getPassword().length() < 6)
            return ResponseEntity.badRequest().body(Map.of("error", "Password kam se kam 6 characters ka hona chahiye."));

        User savedUser = userService.signup(user);
        // Signup pe points
        gamificationService.addPoints(savedUser.getId(), GamificationService.POINTS_PROFILE_SETUP, "signup");

        Map<String, Object> response = new HashMap<>();
        response.put("id",      savedUser.getId());
        response.put("name",    savedUser.getName());
        response.put("email",   savedUser.getEmail());
        response.put("phone",   savedUser.getPhone());
        response.put("message", "Signup successful! Welcome " + savedUser.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (loginRequest.getEmail() == null || loginRequest.getPassword() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Email aur password dono zaroori hain."));

        User user = userService.login(loginRequest.getEmail(), loginRequest.getPassword());
        // Login pe points + streak update
        gamificationService.addPoints(user.getId(), GamificationService.POINTS_LOGIN, "login");

        Map<String, Object> response = new HashMap<>();
        response.put("id",      user.getId());
        response.put("name",    user.getName());
        response.put("email",   user.getEmail());
        response.put("phone",   user.getPhone());
        response.put("message", "Login successful! Welcome back, " + user.getName());
        return ResponseEntity.ok(response);
    }
}