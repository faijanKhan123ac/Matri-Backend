package com.example.health.care.Controller;

import com.example.health.care.Entity.Appointment;
import com.example.health.care.Service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    // POST /api/appointments/book
    @PostMapping("/book")
    public ResponseEntity<?> bookAppointment(@RequestBody Appointment appointment) {
        try {
            if (appointment.getPatientId() == null || appointment.getDoctorId() == null)
                return ResponseEntity.badRequest().body(Map.of("error", "patientId aur doctorId zaroori hain."));
            if (appointment.getDate() == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Date zaroori hai."));
            if (appointment.getTime() == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Time zaroori hai."));
            Appointment booked = appointmentService.bookAppointment(appointment);
            return ResponseEntity.ok(booked);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/appointments/patient/{patientId}
    // Sirf us patient ki appointments — dusre ka ID dene se empty list milegi
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPatientAppointments(@PathVariable Long patientId) {
        try {
            List<Appointment> appointments = appointmentService.getPatientAppointments(patientId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/appointments/doctor/{doctorId}
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> getDoctorAppointments(@PathVariable Long doctorId) {
        try {
            List<Appointment> appointments = appointmentService.getDoctorAppointments(doctorId);
            return ResponseEntity.ok(appointments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/appointments/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long id) {
        try {
            appointmentService.cancelAppointment(id);
            return ResponseEntity.ok(Map.of("message", "Appointment cancel ho gayi."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}