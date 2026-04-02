package com.example.health.care.Service;

import com.example.health.care.Entity.HealthPoints;
import com.example.health.care.Entity.User;
import com.example.health.care.Repository.AppointmentRepository;
import com.example.health.care.Repository.HealthPointsRepository;
import com.example.health.care.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GamificationService {

    @Autowired
    private HealthPointsRepository healthPointsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    // Points system
    public static final int POINTS_LOGIN         = 5;
    public static final int POINTS_APPOINTMENT   = 50;
    public static final int POINTS_REPORT_UPLOAD = 30;
    public static final int POINTS_PROFILE_SETUP = 20;
    public static final int POINTS_STREAK_BONUS  = 10;

    // Get or create health points for user
    public HealthPoints getOrCreate(Long userId) {
        return healthPointsRepository.findByUserId(userId).orElseGet(() -> {
            HealthPoints hp = new HealthPoints();
            hp.setUserId(userId);
            hp.setTotalPoints(0);
            hp.setStreak(0);
            hp.setLastActiveDate(LocalDate.now());
            hp.setUpdatedAt(LocalDateTime.now());
            return healthPointsRepository.save(hp);
        });
    }

    // Add points with reason
    public HealthPoints addPoints(Long userId, int points, String reason) {
        HealthPoints hp = getOrCreate(userId);
        hp.setTotalPoints(hp.getTotalPoints() + points);
        hp.setUpdatedAt(LocalDateTime.now());

        // Streak update
        LocalDate today    = LocalDate.now();
        LocalDate lastDate = hp.getLastActiveDate();
        if (lastDate == null || lastDate.isBefore(today.minusDays(1))) {
            hp.setStreak(1); // Reset streak
        } else if (lastDate.isBefore(today)) {
            hp.setStreak(hp.getStreak() + 1); // Continue streak
            // Streak bonus every 7 days
            if (hp.getStreak() % 7 == 0) {
                hp.setTotalPoints(hp.getTotalPoints() + POINTS_STREAK_BONUS);
            }
        }
        hp.setLastActiveDate(today);
        return healthPointsRepository.save(hp);
    }

    // Get user stats with badges
    public Map<String, Object> getUserStats(Long userId) {
        HealthPoints hp = getOrCreate(userId);
        int points       = hp.getTotalPoints();
        int streak       = hp.getStreak();

        // Level system
        String level   = getLevel(points);
        String nextLevel = getNextLevel(points);
        int nextPoints   = getNextLevelPoints(points);
        int progress     = getProgress(points);

        // Badges earned
        List<Map<String, String>> badges = getBadges(userId, points, streak);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("userId",      userId);
        stats.put("totalPoints", points);
        stats.put("streak",      streak);
        stats.put("level",       level);
        stats.put("nextLevel",   nextLevel);
        stats.put("nextPoints",  nextPoints);
        stats.put("progress",    progress);
        stats.put("badges",      badges);
        return stats;
    }

    private String getLevel(int points) {
        if (points >= 500) return "Health Champion";
        if (points >= 300) return "Wellness Warrior";
        if (points >= 150) return "Health Seeker";
        if (points >= 50)  return "Beginner";
        return "Newbie";
    }

    private String getNextLevel(int points) {
        if (points >= 500) return "Max Level!";
        if (points >= 300) return "Health Champion";
        if (points >= 150) return "Wellness Warrior";
        if (points >= 50)  return "Health Seeker";
        return "Beginner";
    }

    private int getNextLevelPoints(int points) {
        if (points >= 500) return 500;
        if (points >= 300) return 500;
        if (points >= 150) return 300;
        if (points >= 50)  return 150;
        return 50;
    }

    private int getProgress(int points) {
        if (points >= 500) return 100;
        if (points >= 300) return (int) ((points - 300) * 100.0 / 200);
        if (points >= 150) return (int) ((points - 150) * 100.0 / 150);
        if (points >= 50)  return (int) ((points - 50) * 100.0 / 100);
        return (int) (points * 100.0 / 50);
    }

    private List<Map<String, String>> getBadges(Long userId, int points, int streak) {
        List<Map<String, String>> badges = new ArrayList<>();

        // Points badges
        if (points >= 50)  badges.add(badge("First Steps",      "Pehle 50 points kamaye!",        "🌱", "green"));
        if (points >= 150) badges.add(badge("Health Seeker",    "150 points achieve kiye!",       "⭐", "blue"));
        if (points >= 300) badges.add(badge("Wellness Warrior", "300 points — real hero!",        "🏆", "gold"));
        if (points >= 500) badges.add(badge("Health Champion",  "500 points — Legend!",           "👑", "purple"));

        // Appointment badges
        try {
            long apptCount = appointmentRepository.findByPatientId(userId).size();
            if (apptCount >= 1)  badges.add(badge("First Appointment", "Pehli appointment book ki!", "📅", "teal"));
            if (apptCount >= 5)  badges.add(badge("Regular Patient",   "5 appointments complete!",  "🏅", "blue"));
            if (apptCount >= 10) badges.add(badge("Dedicated",         "10 appointments — Wow!",    "💪", "gold"));
        } catch (Exception e) { /* silent */ }

        // Streak badges
        if (streak >= 3)  badges.add(badge("On Fire!",     "3 din streak!",   "🔥", "orange"));
        if (streak >= 7)  badges.add(badge("Week Warrior", "7 din streak!",   "⚡", "yellow"));
        if (streak >= 30) badges.add(badge("Unstoppable",  "30 din streak!",  "🚀", "purple"));

        return badges;
    }

    private Map<String, String> badge(String name, String desc, String icon, String color) {
        Map<String, String> b = new LinkedHashMap<>();
        b.put("name",  name);
        b.put("desc",  desc);
        b.put("icon",  icon);
        b.put("color", color);
        return b;
    }

    // Leaderboard — top 10
    public List<Map<String, Object>> getLeaderboard() {
        List<HealthPoints> top = healthPointsRepository.findTopByPoints();
        return top.stream().limit(10).map(hp -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("userId",      hp.getUserId());
            entry.put("totalPoints", hp.getTotalPoints());
            entry.put("streak",      hp.getStreak());
            entry.put("level",       getLevel(hp.getTotalPoints()));
            // User naam fetch karo
            try {
                User user = userRepository.findById(hp.getUserId()).orElse(null);
                entry.put("name", user != null ? user.getName() : "User #" + hp.getUserId());
            } catch (Exception e) {
                entry.put("name", "User #" + hp.getUserId());
            }
            return entry;
        }).collect(Collectors.toList());
    }
}