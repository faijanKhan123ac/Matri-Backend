package com.example.health.care.Controller;

import com.example.health.care.Entity.Appointment;
import com.example.health.care.Repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.health.care.Service.GamificationService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

/**
 * FileUploadController
 *
 * Patient apni report appointment ke saath attach karta hai.
 * Doctor ki report upload ke liye alag dedicated endpoint hai:
 *   POST /api/medical-reports/doctor-upload   (MedicalReportController)
 *
 * Yahan sirf patient-side upload aur generic file download hai.
 */
@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileUploadController {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private GamificationService gamificationService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    // ================================================================
    // POST /api/files/upload/{appointmentId}
    // Patient apni report appointment ke saath upload kare
    // ================================================================
    @PostMapping("/upload/{appointmentId}")
    public ResponseEntity<?> uploadFile(
            @PathVariable Long appointmentId,
            @RequestParam("file") MultipartFile file) {
        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new RuntimeException("Appointment nahi mili ID: " + appointmentId));

            // File type check
            String contentType = file.getContentType();
            if (contentType == null ||
                    (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Sirf image (JPG/PNG) ya PDF allowed hai."));
            }

            // File size check — max 5MB
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File size 5MB se zyada nahi honi chahiye."));
            }

            // Upload directory create karo
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);

            // Unique filename — patient upload prefix
            String ext      = getExtension(file.getOriginalFilename());
            String fileName = "appt_" + appointmentId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
            Path filePath   = uploadPath.resolve(fileName);

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Appointment record update karo
            appointment.setReportPath(fileName);
            appointment.setReportOriginalName(file.getOriginalFilename());
            appointmentRepository.save(appointment);

            // Gamification: patient ko points do
            try {
                gamificationService.addPoints(
                        appointment.getPatientId(),
                        GamificationService.POINTS_REPORT_UPLOAD,
                        "report_upload");
            } catch (Exception ignored) {}

            return ResponseEntity.ok(Map.of(
                    "message",       "File upload ho gayi!",
                    "fileName",      fileName,
                    "originalName",  file.getOriginalFilename(),
                    "appointmentId", appointmentId
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "File save nahi ho saki: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ================================================================
    // GET /api/files/download/{fileName}
    // Generic file download — patient aur doctor dono ke liye
    // MedicalReportController ka /download/{fileName} bhi same kaam karta hai
    // ================================================================
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
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

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".bin";
        return filename.substring(filename.lastIndexOf("."));
    }
}