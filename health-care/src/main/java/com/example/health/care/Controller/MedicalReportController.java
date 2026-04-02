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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MedicalReportController
 *
 * Doctor appointment ke baad patient ki report upload karta hai.
 * Wahi report patient aur doctor dono apni-apni list mein dekh sakte hain.
 *
 * Endpoints:
 *   POST /api/medical-reports/doctor-upload      — Doctor report upload kare
 *   GET  /api/medical-reports/patient/{patientId} — Patient apni saari reports dekhe
 *   GET  /api/medical-reports/doctor/{doctorId}   — Doctor ne jo reports upload ki wo dekhe
 *   GET  /api/medical-reports/appointment/{id}    — Ek appointment ki saari reports
 *   GET  /api/medical-reports/download/{fileName} — File download/view
 *   DELETE /api/medical-reports/{reportId}        — Doctor report delete kare
 */
@RestController
@RequestMapping("/api/medical-reports")
@CrossOrigin(origins = "*")
public class MedicalReportController {

    @Autowired
    private MedicalReportRepository medicalReportRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // ================================================================
    // POST /api/medical-reports/doctor-upload
    // Doctor appointment ke baad patient ki report upload kare
    // Form fields: file, doctorId, patientId, appointmentId (optional),
    //              reportTitle (optional), doctorNotes (optional)
    // ================================================================
    @PostMapping("/doctor-upload")
    public ResponseEntity<?> doctorUploadReport(
            @RequestParam("file")          MultipartFile file,
            @RequestParam("doctorId")      Long doctorId,
            @RequestParam("patientId")     Long patientId,
            @RequestParam(value = "appointmentId", required = false) Long appointmentId,
            @RequestParam(value = "reportTitle",   required = false, defaultValue = "Medical Report") String reportTitle,
            @RequestParam(value = "doctorNotes",   required = false, defaultValue = "") String doctorNotes) {

        try {
            // --- Validations ---
            if (doctorId == null || patientId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "doctorId aur patientId zaroori hain."));
            }

            // Doctor exist karta hai?
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new RuntimeException("Doctor nahi mila ID: " + doctorId));

            // Patient exist karta hai?
            patientRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient nahi mila ID: " + patientId));

            // File type check — sirf image ya PDF
            String contentType = file.getContentType();
            if (contentType == null ||
                    (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Sirf image (JPG/PNG/JPEG) ya PDF allowed hai."));
            }

            // File size check — max 10MB
            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File size 10MB se zyada nahi honi chahiye."));
            }

            // --- File Save ---
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            String ext      = getExtension(file.getOriginalFilename());
            String fileName = "medreport_doc" + doctorId + "_pat" + patientId
                    + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path filePath   = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // --- Save to DB ---
            MedicalReport report = new MedicalReport();
            report.setDoctorId(doctorId);
            report.setPatientId(patientId);
            report.setAppointmentId(appointmentId);
            report.setFileName(fileName);
            report.setOriginalName(file.getOriginalFilename());
            report.setReportTitle(reportTitle.isBlank() ? "Medical Report" : reportTitle);
            report.setDoctorNotes(doctorNotes);
            report.setFileType(contentType);
            report.setFileSize(file.getSize());
            report.setUploadedAt(LocalDateTime.now());
            report.setUploadedBy("DOCTOR");

            MedicalReport saved = medicalReportRepository.save(report);

            // Appointment table mein bhi flag karo (agar appointmentId diya gaya ho)
            if (appointmentId != null) {
                appointmentRepository.findById(appointmentId).ifPresent(appt -> {
                    appt.setReportPath(fileName);
                    appt.setReportOriginalName(file.getOriginalFilename());
                    appointmentRepository.save(appt);
                });
            }

            return ResponseEntity.ok(Map.of(
                    "message",       "Report successfully upload ho gayi!",
                    "reportId",      saved.getId(),
                    "fileName",      fileName,
                    "originalName",  file.getOriginalFilename(),
                    "reportTitle",   saved.getReportTitle(),
                    "patientId",     patientId,
                    "doctorId",      doctorId,
                    "doctorName",    doctor.getName(),
                    "uploadedAt",    saved.getUploadedAt().toString()
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "File save nahi ho saki: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ================================================================
    // GET /api/medical-reports/patient/{patientId}
    // Patient apni saari doctor-uploaded reports dekhe
    // ================================================================
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getPatientReports(@PathVariable Long patientId) {
        try {
            List<MedicalReport> reports =
                    medicalReportRepository.findByPatientIdOrderByUploadedAtDesc(patientId);

            List<Map<String, Object>> result = reports.stream().map(r -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("reportId",      r.getId());
                map.put("reportTitle",   r.getReportTitle());
                map.put("fileName",      r.getFileName());
                map.put("originalName",  r.getOriginalName() != null ? r.getOriginalName() : "");
                map.put("fileType",      r.getFileType() != null ? r.getFileType() : "");
                map.put("fileSize",      r.getFileSize() != null ? r.getFileSize() : 0);
                map.put("doctorNotes",   r.getDoctorNotes() != null ? r.getDoctorNotes() : "");
                map.put("uploadedAt",    r.getUploadedAt().toString());
                map.put("uploadedBy",    r.getUploadedBy());
                map.put("appointmentId", r.getAppointmentId() != null ? r.getAppointmentId() : "");
                map.put("patientId",     r.getPatientId());
                map.put("doctorId",      r.getDoctorId());

                // Doctor ka naam bhi include karo
                try {
                    Doctor doc = doctorRepository.findById(r.getDoctorId()).orElse(null);
                    map.put("doctorName",          doc != null ? doc.getName() : "Doctor");
                    map.put("doctorSpecialization", doc != null ? doc.getSpecialization() : "");
                    map.put("doctorHospital",       doc != null ? doc.getHospital() : "");
                } catch (Exception ignored) {
                    map.put("doctorName", "Doctor");
                }

                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "patientId",   patientId,
                    "totalReports", result.size(),
                    "reports",     result
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ================================================================
    // GET /api/medical-reports/doctor/{doctorId}
    // Doctor ne jo reports upload ki hain wo dekhe
    // ================================================================
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> getDoctorReports(@PathVariable Long doctorId) {
        try {
            List<MedicalReport> reports =
                    medicalReportRepository.findByDoctorIdOrderByUploadedAtDesc(doctorId);

            List<Map<String, Object>> result = reports.stream().map(r -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("reportId",      r.getId());
                map.put("reportTitle",   r.getReportTitle());
                map.put("fileName",      r.getFileName());
                map.put("originalName",  r.getOriginalName() != null ? r.getOriginalName() : "");
                map.put("fileType",      r.getFileType() != null ? r.getFileType() : "");
                map.put("fileSize",      r.getFileSize() != null ? r.getFileSize() : 0);
                map.put("doctorNotes",   r.getDoctorNotes() != null ? r.getDoctorNotes() : "");
                map.put("uploadedAt",    r.getUploadedAt().toString());
                map.put("appointmentId", r.getAppointmentId() != null ? r.getAppointmentId() : "");
                map.put("patientId",     r.getPatientId());
                map.put("doctorId",      r.getDoctorId());

                // Patient ka naam bhi include karo
                try {
                    Patient patient = patientRepository.findById(r.getPatientId()).orElse(null);
                    if (patient != null && patient.getUser() != null) {
                        User user = userRepository.findById(patient.getUser().getId()).orElse(null);
                        map.put("patientName",  user != null ? user.getName() : "Patient");
                        map.put("patientEmail", user != null ? user.getEmail() : "");
                        map.put("patientAge",   patient.getAge());
                        map.put("patientGender", patient.getGender() != null ? patient.getGender() : "");
                    } else {
                        map.put("patientName", "Patient #" + r.getPatientId());
                    }
                } catch (Exception ignored) {
                    map.put("patientName", "Patient #" + r.getPatientId());
                }

                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "doctorId",     doctorId,
                    "totalReports", result.size(),
                    "reports",      result
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ================================================================
    // GET /api/medical-reports/appointment/{appointmentId}
    // Ek specific appointment ki saari reports
    // ================================================================
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<?> getAppointmentReports(@PathVariable Long appointmentId) {
        try {
            List<MedicalReport> reports =
                    medicalReportRepository.findByAppointmentIdOrderByUploadedAtDesc(appointmentId);

            List<Map<String, Object>> result = reports.stream().map(r -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("reportId",     r.getId());
                map.put("reportTitle",  r.getReportTitle());
                map.put("fileName",     r.getFileName());
                map.put("originalName", r.getOriginalName() != null ? r.getOriginalName() : "");
                map.put("fileType",     r.getFileType() != null ? r.getFileType() : "");
                map.put("doctorNotes",  r.getDoctorNotes() != null ? r.getDoctorNotes() : "");
                map.put("uploadedAt",   r.getUploadedAt().toString());
                map.put("uploadedBy",   r.getUploadedBy());
                return map;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(Map.of(
                    "appointmentId", appointmentId,
                    "totalReports",  result.size(),
                    "reports",       result
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ================================================================
    // GET /api/medical-reports/download/{fileName}
    // File download / inline view
    // ================================================================
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadReport(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) contentType = "application/octet-stream";

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ================================================================
    // DELETE /api/medical-reports/{reportId}?doctorId={doctorId}
    // Sirf wahi doctor delete kar sakta hai jisne upload kiya
    // ================================================================
    @DeleteMapping("/{reportId}")
    public ResponseEntity<?> deleteReport(
            @PathVariable Long reportId,
            @RequestParam Long doctorId) {
        try {
            MedicalReport report = medicalReportRepository.findById(reportId)
                    .orElseThrow(() -> new RuntimeException("Report nahi mili ID: " + reportId));

            if (!report.getDoctorId().equals(doctorId)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "Sirf us doctor ko delete karne ki permission hai jisne upload kiya."));
            }

            // Disk se bhi delete karo
            try {
                Path filePath = Paths.get(uploadDir).toAbsolutePath()
                        .normalize().resolve(report.getFileName());
                Files.deleteIfExists(filePath);
            } catch (IOException ignored) { /* silent */ }

            medicalReportRepository.delete(report);
            return ResponseEntity.ok(Map.of("message", "Report delete ho gayi."));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ================================================================
    // Helper
    // ================================================================
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".bin";
        return filename.substring(filename.lastIndexOf("."));
    }
}