package com.example.health.care.Service;

import com.example.health.care.Entity.Appointment;
import com.example.health.care.Entity.Doctor;
import com.example.health.care.Entity.User;
import com.example.health.care.Repository.AppointmentRepository;
import com.example.health.care.Repository.DoctorRepository;
import com.example.health.care.Repository.PatientRepository;
import com.example.health.care.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.net.InetAddress;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;

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

    // Frontend ka base URL — email mein join link ke liye
    public AppointmentService(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    public Appointment bookAppointment(Appointment appointment) {

        if (appointment.getDoctorId() == null)
            throw new RuntimeException("Doctor ID zaroori hai.");
        if (appointment.getPatientId() == null)
            throw new RuntimeException("Patient ID zaroori hai.");
        if (appointment.getDate() == null)
            throw new RuntimeException("Date zaroori hai.");
        if (appointment.getTime() == null)
            throw new RuntimeException("Time zaroori hai.");

        boolean alreadyBooked = appointmentRepository.existsActiveBooking(
                appointment.getDoctorId(),
                appointment.getDate(),
                appointment.getTime());
        if (alreadyBooked) {
            throw new RuntimeException(
                    "Doctor ke paas " + appointment.getDate() + " ko " +
                            appointment.getTime() + " baje pehle se appointment booked hai. Koi aur time chunein.");
        }

        boolean patientAlreadyBooked = appointmentRepository
                .existsByPatientIdAndDoctorIdAndDate(
                        appointment.getPatientId(),
                        appointment.getDoctorId(),
                        appointment.getDate());
        if (patientAlreadyBooked) {
            throw new RuntimeException(
                    "Aapne is doctor ke saath " + appointment.getDate() + " ko pehle se appointment book kar rakhi hai.");
        }

        Appointment saved = appointmentRepository.save(appointment);

        // Appointment book hone ke baad doctor ko email bhejo
        sendAppointmentEmailToDoctor(saved);

        return saved;
    }

    private void sendAppointmentEmailToDoctor(Appointment appointment) {
        if (mailSender == null || senderEmail.equals("not-configured")) return;

        try {
            // Doctor info lo
            Doctor doctor = doctorRepository.findById(appointment.getDoctorId()).orElse(null);
            if (doctor == null || doctor.getEmail() == null || doctor.getEmail().isBlank()) return;

            // Patient info lo
            String patientName = "Patient";
            try {
                var patient = patientRepository.findById(appointment.getPatientId()).orElse(null);
                if (patient != null) {
                    User user = userRepository.findById(patient.getUser().getId()).orElse(null);
                    if (user != null) patientName = user.getName();
                }
            } catch (Exception e) { /* ignore */ }

            String dateStr = appointment.getDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            String timeStr = appointment.getTime().format(DateTimeFormatter.ofPattern("hh:mm a"));

            // Server ka actual IP detect karo taaki doctor kisi bhi device se join kar sake
            String serverIp = "localhost";
            try { serverIp = InetAddress.getLocalHost().getHostAddress(); } catch (Exception ignored) {}

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(doctor.getEmail());
            helper.setSubject("📅 Naya Appointment — " + patientName + " | " + dateStr + " " + timeStr);

            String html = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:20px'>"
                    + "<div style='max-width:600px;margin:0 auto;background:white;border-radius:12px;overflow:hidden'>"

                    // Header
                    + "<div style='background:#00b4a0;padding:28px;text-align:center'>"
                    + "<h1 style='color:white;margin:0;font-size:24px'>📅 Naya Appointment!</h1>"
                    + "</div>"

                    // Body
                    + "<div style='padding:30px'>"
                    + "<p style='font-size:16px;color:#333'>Namaste <b>Dr. " + doctor.getName() + "</b> ji! 🙏</p>"
                    + "<p style='font-size:15px;color:#555'>Ek naya appointment book hua hai:</p>"

                    // Details box
                    + "<div style='background:#f0faf9;border:1px solid #00b4a0;border-radius:10px;padding:20px;margin:20px 0'>"
                    + "<table style='width:100%;border-collapse:collapse'>"
                    + "<tr><td style='padding:8px 0;color:#666;width:40%'>👤 Patient</td><td style='padding:8px 0;font-weight:bold'>" + patientName + "</td></tr>"
                    + "<tr><td style='padding:8px 0;color:#666'>📅 Date</td><td style='padding:8px 0;font-weight:bold'>" + dateStr + "</td></tr>"
                    + "<tr><td style='padding:8px 0;color:#666'>⏰ Time</td><td style='padding:8px 0;font-weight:bold'>" + timeStr + "</td></tr>"
                    + "<tr><td style='padding:8px 0;color:#666'>🆔 Appointment ID</td><td style='padding:8px 0;font-weight:bold'>" + appointment.getId() + "</td></tr>"
                    + (appointment.getNotes() != null && !appointment.getNotes().isEmpty()
                    ? "<tr><td style='padding:8px 0;color:#666'>📝 Notes</td><td style='padding:8px 0'>" + appointment.getNotes() + "</td></tr>" : "")
                    + "</table>"
                    + "</div>"

                    // Video call section — sirf Room ID dikhao, no broken links
                    + "<div style='background:#e8f4fd;border:2px solid #3498db;border-radius:10px;padding:20px;margin:20px 0;text-align:center'>"
                    + "<p style='color:#1a5276;font-size:16px;margin:0 0 12px;font-weight:bold'>📹 Video Call Kaise Join Karein</p>"
                    + "<div style='background:#3498db;color:white;padding:20px 30px;border-radius:12px;display:inline-block;margin-bottom:15px'>"
                    + "<p style='margin:0;font-size:13px;opacity:0.9;letter-spacing:1px'>ROOM ID</p>"
                    + "<p style='margin:8px 0 0;font-size:52px;font-weight:bold;letter-spacing:8px'>" + appointment.getId() + "</p>"
                    + "</div>"
                    + "</div>"

                    + "<div style='background:#fff9e6;border-left:4px solid #f39c12;padding:15px;border-radius:0 8px 8px 0;margin:15px 0'>"
                    + "<p style='margin:0;color:#856404;font-size:14px;line-height:1.8'>"
                    + "📋 <b>Steps:</b><br>"
                    + "1️⃣ HealthCare App kholo<br>"
                    + "2️⃣ <b>Doctor Login</b> karo<br>"
                    + "3️⃣ <b>Video</b> tab pe click karo<br>"
                    + "4️⃣ Apna naam daalo<br>"
                    + "5️⃣ Room ID <b style='font-size:20px;color:#e74c3c'>" + appointment.getId() + "</b> daalo<br>"
                    + "6️⃣ <b>Join Video Call</b> button dabao ✅"
                    + "</p>"
                    + "</div>"

                    + "<p style='color:#999;font-size:12px;text-align:center;margin-top:25px'>Yeh notification HealthCare App ne bheja hai.</p>"
                    + "</div></div></body></html>";

            helper.setText(html, true);
            mailSender.send(message);
            System.out.println("[Appointment] Email sent to doctor: " + doctor.getEmail());

        } catch (Exception e) {
            System.out.println("[Appointment] Doctor email send nahi ho saka: " + e.getMessage());
        }
    }

    public List<Appointment> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getDoctorAppointments(Long doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    public void cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment nahi mili ID: " + appointmentId));
        appointmentRepository.delete(appointment);
    }
}