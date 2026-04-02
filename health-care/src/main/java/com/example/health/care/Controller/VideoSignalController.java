package com.example.health.care.Controller;

import com.example.health.care.Dto.VideoSignalMessage;
import com.example.health.care.Entity.Appointment;
import com.example.health.care.Repository.AppointmentRepository;
import com.example.health.care.Repository.PatientRepository;
import com.example.health.care.Repository.UserRepository;
import com.example.health.care.Entity.Patient;
import com.example.health.care.Entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.*;
import java.util.concurrent.*;

@Controller
public class VideoSignalController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    private final Map<String, Set<String>> rooms = new ConcurrentHashMap<>();

    @MessageMapping("/signal")
    public void handleSignal(@Payload VideoSignalMessage message) {
        String roomId = message.getRoomId();
        if (roomId == null) return;

        rooms.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>());

        if ("join".equals(message.getType())) {
            rooms.get(roomId).add(message.getSenderId());

            // Patient join hua toh doctor ko notify karo
            // Doctor ka topic: /topic/doctor-notify/{doctorId}
            try {
                Long apptId = Long.parseLong(roomId);
                Appointment appt = appointmentRepository.findById(apptId).orElse(null);
                if (appt != null) {
                    // Patient ka naam fetch karo
                    String patientName = message.getSenderName();
                    try {
                        Patient patient = patientRepository.findById(appt.getPatientId()).orElse(null);
                        if (patient != null && patient.getUser() != null) {
                            User user = userRepository.findById(patient.getUser().getId()).orElse(null);
                            if (user != null) patientName = user.getName();
                        }
                    } catch (Exception e) { /* silent */ }

                    // Doctor ko notification bhejo
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("type",        "patient-joined");
                    notification.put("roomId",       roomId);
                    notification.put("patientName",  patientName);
                    notification.put("appointmentId", apptId);
                    notification.put("message",      patientName + " Video Room #" + roomId + " mein aa gaye hain!");

                    messagingTemplate.convertAndSend(
                            "/topic/doctor-notify/" + appt.getDoctorId(),
                            Optional.of(notification)
                    );
                    System.out.println("[Video] Patient joined notify sent to doctor: " + appt.getDoctorId());
                }
            } catch (Exception e) { /* roomId number nahi hai */ }

        } else if ("leave".equals(message.getType())) {
            rooms.get(roomId).remove(message.getSenderId());
        }

        // Room ke sabhi members ko broadcast karo
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }
}