package com.example.health.care.Controller;

import com.example.health.care.Entity.Doctor;
import com.example.health.care.Entity.Patient;
import com.example.health.care.Service.DoctorService;
import com.example.health.care.Service.PatientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patient")
@CrossOrigin(origins = "*")
public class PatientController {

    @Autowired
    private PatientService patientService;

    @Autowired
    private DoctorService doctorService;

    // POST /api/patient/register
    @PostMapping("/register")
    public ResponseEntity<?> registerPatient(@RequestBody Patient patient) {
        try {
            if (patient.getUser() == null || patient.getUser().getId() == null)
                return ResponseEntity.badRequest().body(Map.of("error", "User ID zaroori hai."));
            if (patient.getAge() <= 0)
                return ResponseEntity.badRequest().body(Map.of("error", "Valid age dalo."));
            if (patient.getGender() == null || patient.getGender().isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Gender zaroori hai."));

            Patient saved = patientService.registerPatient(patient);

            // Entity directly return karne se infinite recursion hoti hai (User -> Patient -> User)
            // Isliye sirf zaroori fields return karo
            return ResponseEntity.ok(Map.of(
                    "id",       saved.getId(),
                    "userId",   saved.getUser().getId(),
                    "age",      saved.getAge(),
                    "gender",   saved.getGender(),
                    "disease",  saved.getDisease() != null ? saved.getDisease() : "",
                    "latitude", saved.getLatitude(),
                    "longitude",saved.getLongitude(),
                    "message",  "Patient registered successfully!"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/patient/user/{userId}
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getPatientByUserId(@PathVariable Long userId) {
        try {
            Patient patient = patientService.getPatientByUserId(userId);
            return ResponseEntity.ok(Map.of(
                    "id",        patient.getId(),
                    "userId",    patient.getUser().getId(),
                    "age",       patient.getAge(),
                    "gender",    patient.getGender(),
                    "disease",   patient.getDisease() != null ? patient.getDisease() : "",
                    "latitude",  patient.getLatitude(),
                    "longitude", patient.getLongitude()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/patient/{patientId}/location
    @PutMapping("/{patientId}/location")
    public ResponseEntity<?> updateLocation(
            @PathVariable Long patientId,
            @RequestBody Map<String, Double> locationData) {
        try {
            Double latitude  = locationData.get("latitude");
            Double longitude = locationData.get("longitude");
            if (latitude == null || longitude == null)
                return ResponseEntity.badRequest().body(Map.of("error", "latitude aur longitude zaroori hain."));
            Patient updated = patientService.updateLocation(patientId, latitude, longitude);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/patient/{patientId}/nearby-doctors
    @GetMapping("/{patientId}/nearby-doctors")
    public ResponseEntity<?> getNearbyDoctors(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "10.0") double radiusKm) {
        try {
            Patient patient = patientService.getPatientById(patientId);
            List<Doctor> nearbyDoctors = doctorService.getNearbyDoctors(
                    patient.getLatitude(), patient.getLongitude(), radiusKm);
            return ResponseEntity.ok(Map.of(
                    "patientId",  patientId,
                    "totalFound", nearbyDoctors.size(),
                    "doctors",    nearbyDoctors));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}