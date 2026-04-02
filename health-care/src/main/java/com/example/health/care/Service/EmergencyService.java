package com.example.health.care.Service;

import com.example.health.care.Entity.FamilyMember;
import com.example.health.care.Entity.User;
import com.example.health.care.Repository.FamilyMemberrepository;
import com.example.health.care.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.util.List;

@Service
public class EmergencyService {

    @Autowired
    private FamilyMemberrepository familyMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)  // App crash nahi karega agar mail dependency na ho
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:not-configured}")
    private String senderEmail;

    public FamilyMember addFamilyMember(FamilyMember member) {
        if (member.getName() == null || member.getName().isBlank())
            throw new RuntimeException("Naam zaroori hai.");
        if (member.getEmail() == null || member.getEmail().isBlank())
            throw new RuntimeException("Email zaroori hai.");
        if (member.getRelation() == null || member.getRelation().isBlank())
            throw new RuntimeException("Relation zaroori hai.");
        return familyMemberRepository.save(member);
    }

    public List<FamilyMember> getFamilyMembers(Long userId) {
        return familyMemberRepository.findByUserId(userId);
    }

    public void deleteFamilyMember(Long memberId, Long userId) {
        familyMemberRepository.deleteByIdAndUserId(memberId, userId);
    }

    public String sendEmergencyAlert(Long userId, Double latitude, Double longitude) {

        if (mailSender == null || senderEmail.equals("not-configured")) {
            throw new RuntimeException(
                    "Email configure nahi hai. application.properties mein " +
                            "spring.mail.username aur spring.mail.password set karo.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User nahi mila."));

        List<FamilyMember> members = familyMemberRepository.findByUserId(userId);
        if (members.isEmpty()) {
            throw new RuntimeException(
                    "Koi family member registered nahi hai. Pehle SOS page se family members add karo.");
        }

        String mapsLink = (latitude != null && longitude != null)
                ? "https://www.google.com/maps?q=" + latitude + "," + longitude
                : null;

        String locationHtml = mapsLink != null
                ? "<a href='" + mapsLink + "' style='color:#e74c3c;font-weight:bold'>📍 Location Map Link Kholein</a>"
                : "Location available nahi hai.";

        int sent = 0;
        StringBuilder errors = new StringBuilder();

        for (FamilyMember member : members) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(senderEmail);
                helper.setTo(member.getEmail());
                helper.setSubject("🚨 EMERGENCY ALERT — " + user.getName() + " ko madad chahiye!");

                String html = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:20px'>"
                        + "<div style='max-width:600px;margin:0 auto;background:white;border-radius:12px;overflow:hidden'>"
                        + "<div style='background:#e74c3c;padding:30px;text-align:center'>"
                        + "<h1 style='color:white;margin:0;font-size:32px'>🚨 EMERGENCY ALERT</h1>"
                        + "</div>"
                        + "<div style='padding:30px'>"
                        + "<p style='font-size:18px'>Namaste <b>" + member.getName() + "</b> ji,</p>"
                        + "<div style='background:#ffeaea;border-left:4px solid #e74c3c;padding:15px;border-radius:8px;margin:15px 0'>"
                        + "<p style='color:#e74c3c;font-size:17px;margin:0;font-weight:bold'>"
                        + "⚠️ Aapke " + member.getRelation() + " <b>" + user.getName() + "</b> ko abhi EMERGENCY hai!</p>"
                        + "</div>"
                        + "<table style='width:100%;background:#f9f9f9;border-radius:8px;padding:15px;margin:20px 0;border-collapse:collapse'>"
                        + "<tr><td style='padding:10px;color:#666'>👤 Naam</td><td style='padding:10px;font-weight:bold'>" + user.getName() + "</td></tr>"
                        + "<tr><td style='padding:10px;color:#666'>📱 Phone</td><td style='padding:10px;font-weight:bold'>" + (user.getPhone() != null ? user.getPhone() : "N/A") + "</td></tr>"
                        + "<tr><td style='padding:10px;color:#666'>📧 Email</td><td style='padding:10px'>" + user.getEmail() + "</td></tr>"
                        + "<tr><td style='padding:10px;color:#666'>📍 Location</td><td style='padding:10px'>" + locationHtml + "</td></tr>"
                        + "</table>"
                        + (mapsLink != null ? "<div style='text-align:center;margin:20px 0'>"
                        + "<a href='" + mapsLink + "' style='background:#e74c3c;color:white;padding:14px 28px;border-radius:8px;text-decoration:none;font-size:16px;font-weight:bold'>"
                        + "📍 Map Pe Location Dekho</a></div>" : "")
                        + "<p style='color:#999;font-size:12px;text-align:center;margin-top:30px'>Yeh alert HealthCare Emergency App se bheja gaya.</p>"
                        + "</div></div></body></html>";

                helper.setText(html, true);
                mailSender.send(message);
                sent++;
            } catch (Exception e) {
                errors.append(member.getName()).append(" (").append(member.getEmail()).append("): ").append(e.getMessage()).append("; ");
            }
        }

        if (sent == 0) {
            throw new RuntimeException("Email send nahi ho saki. Error: " + errors);
        }

        return sent + "/" + members.size() + " family member(s) ko emergency alert bhej diya! " +
                (errors.length() > 0 ? "Kuch fail: " + errors : "");
    }
}