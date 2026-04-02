package com.example.health.care.Controller;

import com.example.health.care.Entity.Appointment;
import com.example.health.care.Entity.Doctor;
import com.example.health.care.Entity.MedicalReport;
import com.example.health.care.Entity.Patient;
import com.example.health.care.Entity.User;
import com.example.health.care.Repository.AppointmentRepository;
import com.example.health.care.Repository.DoctorRepository;
import com.example.health.care.Repository.MedicalReportRepository;
import com.example.health.care.Repository.PatientRepository;
import com.example.health.care.Repository.UserRepository;
import com.example.health.care.Service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "*")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MedicalReportRepository medicalReportRepository;  // NEW — doctor reports

    @Value("${app.frontend.url:http://localhost:5500}")
    private String frontendUrl;

    // POST /api/doctors/register
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Doctor doctor) {
        try {
            if (doctor.getName() == null || doctor.getName().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Naam zaroori hai."));
            if (doctor.getSpecialization() == null || doctor.getSpecialization().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Specialization zaroori hai."));
            if (doctor.getHospital() == null || doctor.getHospital().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Hospital zaroori hai."));
            if (doctor.getPhone() == null || doctor.getPhone().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Phone zaroori hai."));

            Doctor saved = doctorService.registerDoctor(doctor);
            return ResponseEntity.ok(Map.of(
                    "message",        "Doctor registered successfully!",
                    "id",             saved.getId(),
                    "name",           saved.getName(),
                    "specialization", saved.getSpecialization(),
                    "hospital",       saved.getHospital()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /api/doctors/login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String email    = body.get("email");
            String password = body.get("password");
            if (email == null || password == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Email aur password zaroori hain."));

            Doctor doctor = doctorService.loginDoctor(email, password);
            return ResponseEntity.ok(Map.of(
                    "message",        "Login successful!",
                    "id",             doctor.getId(),
                    "name",           doctor.getName(),
                    "email",          doctor.getEmail(),
                    "specialization", doctor.getSpecialization(),
                    "hospital",       doctor.getHospital(),
                    "phone",          doctor.getPhone()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ================================================================
    // GET /api/doctors/{doctorId}/appointments
    // Doctor ke saare appointments — ab medical reports bhi include hain
    // ================================================================
    @GetMapping("/{doctorId}/appointments")
    public ResponseEntity<?> getDoctorAppointments(@PathVariable Long doctorId) {
        try {
            List<Appointment> appointments = appointmentRepository.findByDoctorId(doctorId);

            List<Map<String, Object>> result = appointments.stream().map(a -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id",                 a.getId());
                map.put("patientId",          a.getPatientId());
                map.put("doctorId",           a.getDoctorId());
                map.put("date",               a.getDate().toString());
                map.put("time",               a.getTime().toString());
                map.put("notes",              a.getNotes() != null ? a.getNotes() : "");
                map.put("status",             a.getStatus().toString());
                // Patient-uploaded report (existing fields)
                map.put("reportPath",         a.getReportPath() != null ? a.getReportPath() : "");
                map.put("reportOriginalName", a.getReportOriginalName() != null ? a.getReportOriginalName() : "");

                // Patient details
                try {
                    Optional<Patient> patientOpt = patientRepository.findById(a.getPatientId());
                    if (patientOpt.isPresent()) {
                        Patient patient = patientOpt.get();
                        map.put("patientAge",     patient.getAge());
                        map.put("patientGender",  patient.getGender());
                        map.put("patientDisease", patient.getDisease() != null ? patient.getDisease() : "");
                        if (patient.getUser() != null) {
                            Optional<User> userOpt = userRepository.findById(patient.getUser().getId());
                            if (userOpt.isPresent()) {
                                User user = userOpt.get();
                                map.put("patientName",  user.getName());
                                map.put("patientEmail", user.getEmail());
                                map.put("patientPhone", user.getPhone() != null ? user.getPhone() : "");
                            }
                        }
                    }
                } catch (Exception e) { /* silent */ }

                // Doctor-uploaded medical reports for this appointment — NEW
                try {
                    List<MedicalReport> docReports =
                            medicalReportRepository.findDoctorReportsByAppointment(a.getId());
                    List<Map<String, Object>> reportList = docReports.stream().map(r -> {
                        Map<String, Object> rm = new LinkedHashMap<>();
                        rm.put("reportId",     r.getId());
                        rm.put("reportTitle",  r.getReportTitle());
                        rm.put("fileName",     r.getFileName());
                        rm.put("originalName", r.getOriginalName() != null ? r.getOriginalName() : "");
                        rm.put("doctorNotes",  r.getDoctorNotes() != null ? r.getDoctorNotes() : "");
                        rm.put("uploadedAt",   r.getUploadedAt().toString());
                        return rm;
                    }).collect(Collectors.toList());
                    map.put("doctorReports", reportList);  // NEW field
                } catch (Exception e) {
                    map.put("doctorReports", List.of());
                }

                return map;
            }).collect(Collectors.toList());

            // Latest pehle
            result.sort((a, b) -> b.get("id").toString().compareTo(a.get("id").toString()));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/doctors/all
    @GetMapping("/all")
    public ResponseEntity<?> getAllDoctors() {
        try {
            return ResponseEntity.ok(doctorService.getAllDoctors());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/doctors/nearby
    @GetMapping("/nearby")
    public ResponseEntity<?> getNearbyDoctors(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false) Double radiusKm) {
        try {
            List<Doctor> doctors = doctorService.getNearbyDoctors(latitude, longitude, radiusKm);
            if (doctors.isEmpty())
                return ResponseEntity.ok(Map.of("message", "Koi doctor nahi mila.", "doctors", List.of()));
            return ResponseEntity.ok(doctors);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/doctors/{id}/location
    @PutMapping("/{id}/location")
    public ResponseEntity<?> updateLocation(@PathVariable Long id, @RequestBody Map<String, Double> body) {
        try {
            Doctor updated = doctorService.updateLocation(id, body.get("latitude"), body.get("longitude"));
            return ResponseEntity.ok(Map.of("message", "Location update ho gayi.", "id", updated.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/doctors/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getDoctor(@PathVariable Long id) {
        try {
            Doctor d = doctorService.getDoctorById(id);
            return ResponseEntity.ok(Map.of(
                    "id",             d.getId(),
                    "name",           d.getName(),
                    "specialization", d.getSpecialization(),
                    "hospital",       d.getHospital(),
                    "phone",          d.getPhone(),
                    "email",          d.getEmail() != null ? d.getEmail() : ""
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}