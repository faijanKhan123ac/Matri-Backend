package com.example.health.care.Controller;

import com.example.health.care.Entity.Appointment;
import com.example.health.care.Entity.Doctor;
import com.example.health.care.Entity.Patient;
import com.example.health.care.Entity.User;
import com.example.health.care.Repository.AppointmentRepository;
import com.example.health.care.Repository.DoctorRepository;
import com.example.health.care.Repository.PatientRepository;
import com.example.health.care.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import jakarta.mail.internet.MimeMessage;
import java.net.InetAddress;
import java.util.Map;

@RestController
@RequestMapping("/api/video")
@CrossOrigin(origins = "*")
public class VideoCallController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:not-configured}")
    private String senderEmail;

    @Value("${app.frontend.url:http://localhost:8081}")
    private String frontendUrl;

    // POST /api/video/notify/{appointmentId}
    // Patient jab bhi join kare — doctor ko email bhejo
    @PostMapping("/notify/{appointmentId}")
    public ResponseEntity<?> notifyDoctor(
            @PathVariable Long appointmentId,
            @RequestBody Map<String, String> body) {
        try {
            if (mailSender == null || senderEmail.equals("not-configured"))
                return ResponseEntity.ok(Map.of("message", "Mail not configured."));

            Appointment appt = appointmentRepository.findById(appointmentId).orElse(null);
            if (appt == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Appointment nahi mili."));

            // Doctor email fetch karo
            Doctor doctor = doctorRepository.findById(appt.getDoctorId()).orElse(null);
            if (doctor == null || doctor.getEmail() == null || doctor.getEmail().isBlank())
                return ResponseEntity.ok(Map.of("message", "Doctor email nahi hai."));

            // Patient naam fetch karo
            String patientName = body.getOrDefault("patientName", "Patient");
            try {
                Patient patient = patientRepository.findById(appt.getPatientId()).orElse(null);
                if (patient != null && patient.getUser() != null) {
                    User user = userRepository.findById(patient.getUser().getId()).orElse(null);
                    if (user != null) patientName = user.getName();
                }
            } catch (Exception e) { /* silent */ }

            // Server ka actual IP detect karo
            String serverIp = "localhost";
            try {
                serverIp = InetAddress.getLocalHost().getHostAddress();
            } catch (Exception ignored) {}

            // Email bhejo
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(doctor.getEmail());
            helper.setSubject("📹 Patient Video Call mein aa gaya! — Room #" + appointmentId);

            String html = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:20px'>"
                    + "<div style='max-width:600px;margin:0 auto;background:white;border-radius:12px;overflow:hidden'>"
                    + "<div style='background:#3498db;padding:25px;text-align:center'>"
                    + "<h1 style='color:white;margin:0;font-size:24px'>📹 Patient Video Call Mein Hai!</h1>"
                    + "</div>"
                    + "<div style='padding:30px'>"
                    + "<p style='font-size:17px;color:#333'>Namaste <b>Dr. " + doctor.getName() + "</b> ji! 🙏</p>"
                    + "<div style='background:#e8f4fd;border:2px solid #3498db;border-radius:10px;padding:20px;margin:20px 0;text-align:center'>"
                    + "<p style='color:#1a5276;font-size:15px;margin:0 0 8px;font-weight:bold'>"
                    + "🟢 " + patientName + " Video Room mein aapka wait kar rahe hain!</p>"
                    + "<div style='background:#3498db;color:white;padding:15px 25px;border-radius:10px;display:inline-block;margin-top:10px'>"
                    + "<p style='margin:0;font-size:13px;opacity:0.9'>ROOM ID</p>"
                    + "<p style='margin:6px 0 0;font-size:48px;font-weight:bold;letter-spacing:8px'>" + appointmentId + "</p>"
                    + "</div>"
                    + "<br><br>"
                    + "<a href='http://" + serverIp + ":8081/index.html?joinRoom=" + appointmentId + "&name=Doctor' "
                    + "style='display:inline-block;background:#2ecc71;color:white;padding:16px 32px;"
                    + "border-radius:10px;text-decoration:none;font-size:18px;font-weight:bold;margin-top:10px'>"
                    + "&#128249; Abhi Join Karo"
                    + "</a>"
                    + "</div>"
                    + "<div style='background:#fff9e6;border-left:4px solid #f39c12;padding:15px;border-radius:0 8px 8px 0;margin:15px 0'>"
                    + "<p style='margin:0;color:#856404;font-size:14px;line-height:1.8'>"
                    + "📋 <b>Abhi join karo:</b><br>"
                    + "1️⃣ HealthCare App kholo<br>"
                    + "2️⃣ Doctor Login karo<br>"
                    + "3️⃣ <b>Video</b> tab pe click karo<br>"
                    + "4️⃣ Apna naam daalo<br>"
                    + "5️⃣ Room ID <b style='font-size:22px;color:#e74c3c'>" + appointmentId + "</b> daalo<br>"
                    + "6️⃣ <b>Join Video Call</b> dabao ✅"
                    + "</p>"
                    + "</div>"
                    + "<p style='color:#999;font-size:12px;text-align:center;margin-top:20px'>HealthCare App — Video Consultation</p>"
                    + "</div></div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);

            return ResponseEntity.ok(Map.of(
                    "message", "Doctor ko notification bhej di — " + doctor.getEmail()
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}