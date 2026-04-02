package com.example.health.care.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Doctor appointment ke baad patient ki medical report store karne ke liye.
 * Ek hi file patientId aur doctorId dono se linked hoti hai —
 * isliye dono apni apni list mein yeh report dekh sakte hain.
 */
@Entity
@Table(name = "medical_reports")
public class MedicalReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Kis appointment ke baad upload ki — optional but useful
    @Column(name = "appointment_id")
    private Long appointmentId;

    // Patient ka ID — patient ki report list mein aayegi
    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    // Doctor ka ID — doctor ki uploaded list mein aayega
    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    // Disk pe stored filename (unique)
    @Column(name = "file_name", nullable = false)
    private String fileName;

    // Original file name jo user ne upload kiya
    @Column(name = "original_name")
    private String originalName;

    // Doctor ke notes / diagnosis summary
    @Column(name = "doctor_notes", columnDefinition = "TEXT")
    private String doctorNotes;

    // Report title — e.g. "Blood Test Report", "Ultrasound"
    @Column(name = "report_title")
    private String reportTitle;

    // MIME type for proper download/view
    @Column(name = "file_type")
    private String fileType;

    // File size in bytes
    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    // Who uploaded — "DOCTOR" or "PATIENT"
    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy = "DOCTOR";

    // ===================== Constructors =====================

    public MedicalReport() {}

    // ===================== Getters & Setters =====================

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getDoctorNotes() { return doctorNotes; }
    public void setDoctorNotes(String doctorNotes) { this.doctorNotes = doctorNotes; }

    public String getReportTitle() { return reportTitle; }
    public void setReportTitle(String reportTitle) { this.reportTitle = reportTitle; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
}