package com.example.health.care.Service;

import com.example.health.care.Entity.Appointment;
import com.example.health.care.Entity.Doctor;
import com.example.health.care.Entity.User;
import com.example.health.care.Repository.AppointmentRepository;
import com.example.health.care.Repository.DoctorRepository;
import com.example.health.care.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReminderScheduler {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:not-configured}")
    private String senderEmail;

    // Har roz subah 8 baje — "0 0 8 * * *"
    // Testing ke liye har 60 second: "0 * * * * *"
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Kolkata")
    public void sendAppointmentReminders() {

        if (mailSender == null || senderEmail.equals("not-configured")) {
            System.out.println("[Reminder] Email configure nahi hai — reminder skip.");
            return;
        }

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Appointment> appointments = appointmentRepository.findTomorrowAppointments(tomorrow);

        if (appointments.isEmpty()) {
            System.out.println("[Reminder] Kal koi appointment nahi hai.");
            return;
        }

        System.out.println("[Reminder] Kal ki " + appointments.size() + " appointments mili — reminders bhej rahe hain...");

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

        int sent = 0;
        for (Appointment appt : appointments) {
            try {
                // Patient info lo
                User patient = userRepository.findById(appt.getPatientId()).orElse(null);
                if (patient == null || patient.getEmail() == null) continue;

                // Doctor info lo
                Doctor doctor = doctorRepository.findById(appt.getDoctorId()).orElse(null);
                String doctorName    = doctor != null ? doctor.getName() : "Doctor";
                String hospital      = doctor != null ? doctor.getHospital() : "";
                String doctorPhone   = doctor != null ? doctor.getPhone() : "N/A";
                String specialization = doctor != null ? doctor.getSpecialization() : "";

                String dateStr = appt.getDate().format(dateFormatter);
                String timeStr = appt.getTime().format(timeFormatter);

                // Email bhejo
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(senderEmail);
                helper.setTo(patient.getEmail());
                helper.setSubject("🔔 Appointment Reminder — Kal " + dateStr + " ko " + timeStr);

                String html = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f5f5f5;margin:0;padding:20px'>"
                        + "<div style='max-width:600px;margin:0 auto;background:white;border-radius:12px;overflow:hidden'>"

                        // Header
                        + "<div style='background:#00b4a0;padding:28px;text-align:center'>"
                        + "<h1 style='color:white;margin:0;font-size:26px'>🔔 Appointment Reminder</h1>"
                        + "<p style='color:rgba(255,255,255,0.9);margin:6px 0 0;font-size:15px'>Kal aapki appointment hai!</p>"
                        + "</div>"

                        // Body
                        + "<div style='padding:30px'>"
                        + "<p style='font-size:17px;color:#333'>Namaste <b>" + patient.getName() + "</b> ji! 🙏</p>"
                        + "<p style='font-size:15px;color:#555'>Aapko yaad dilana chahte hain ki <b>kal aapki doctor appointment</b> hai.</p>"

                        // Appointment details box
                        + "<div style='background:#f0faf9;border:1px solid #00b4a0;border-radius:10px;padding:20px;margin:20px 0'>"
                        + "<h3 style='color:#00b4a0;margin:0 0 15px;font-size:16px'>📋 Appointment Details</h3>"
                        + "<table style='width:100%;border-collapse:collapse'>"
                        + "<tr><td style='padding:8px 0;color:#666;width:40%'>📅 Date</td>"
                        + "    <td style='padding:8px 0;font-weight:bold;color:#333'>" + dateStr + "</td></tr>"
                        + "<tr><td style='padding:8px 0;color:#666'>⏰ Time</td>"
                        + "    <td style='padding:8px 0;font-weight:bold;color:#333'>" + timeStr + "</td></tr>"
                        + "<tr><td style='padding:8px 0;color:#666'>👨‍⚕️ Doctor</td>"
                        + "    <td style='padding:8px 0;font-weight:bold;color:#333'>" + doctorName + "</td></tr>"
                        + (specialization.isEmpty() ? "" :
                        "<tr><td style='padding:8px 0;color:#666'>🩺 Specialization</td>"
                                + "    <td style='padding:8px 0;color:#333'>" + specialization + "</td></tr>")
                        + (hospital.isEmpty() ? "" :
                        "<tr><td style='padding:8px 0;color:#666'>🏥 Hospital</td>"
                                + "    <td style='padding:8px 0;color:#333'>" + hospital + "</td></tr>")
                        + "<tr><td style='padding:8px 0;color:#666'>📱 Doctor Phone</td>"
                        + "    <td style='padding:8px 0;color:#333'>" + doctorPhone + "</td></tr>"
                        + (appt.getNotes() != null && !appt.getNotes().isEmpty() ?
                        "<tr><td style='padding:8px 0;color:#666'>📝 Notes</td>"
                                + "    <td style='padding:8px 0;color:#333'>" + appt.getNotes() + "</td></tr>" : "")
                        + "</table>"
                        + "</div>"

                        // Tips
                        + "<div style='background:#fff9e6;border-left:4px solid #f39c12;padding:15px;border-radius:0 8px 8px 0;margin:15px 0'>"
                        + "<p style='margin:0;color:#856404;font-size:14px'>💡 <b>Yaad rakho:</b> Appointment se 10-15 minute pehle pahunch jaao. Apni purani reports aur prescriptions saath le jaao.</p>"
                        + "</div>"

                        + "<p style='color:#999;font-size:12px;text-align:center;margin-top:25px'>Yeh reminder HealthCare App ne automatically bheja hai.</p>"
                        + "</div></div></body></html>";

                helper.setText(html, true);
                mailSender.send(message);
                sent++;
                System.out.println("[Reminder] Sent to: " + patient.getEmail());

            } catch (Exception e) {
                System.out.println("[Reminder] Failed for appointment " + appt.getId() + ": " + e.getMessage());
            }
        }

        System.out.println("[Reminder] Done — " + sent + "/" + appointments.size() + " reminders sent.");
    }
}