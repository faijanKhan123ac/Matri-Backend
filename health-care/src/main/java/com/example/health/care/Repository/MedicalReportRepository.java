package com.example.health.care.Repository;

import com.example.health.care.Entity.MedicalReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalReportRepository extends JpaRepository<MedicalReport, Long> {

    // Patient ki saari reports — newest first
    List<MedicalReport> findByPatientIdOrderByUploadedAtDesc(Long patientId);

    // Doctor ne upload ki saari reports — newest first
    List<MedicalReport> findByDoctorIdOrderByUploadedAtDesc(Long doctorId);

    // Kisi specific appointment ki saari reports
    List<MedicalReport> findByAppointmentIdOrderByUploadedAtDesc(Long appointmentId);

    // Patient + Doctor combination (ek dusre ka shared history)
    List<MedicalReport> findByPatientIdAndDoctorIdOrderByUploadedAtDesc(Long patientId, Long doctorId);

    // Count — patient ke paas kitni reports hain
    long countByPatientId(Long patientId);

    // Count — doctor ne kitni reports upload ki hain
    long countByDoctorId(Long doctorId);

    // Appointment ki latest report (doctor ne upload ki ho)
    @Query("SELECT r FROM MedicalReport r WHERE r.appointmentId = :appointmentId AND r.uploadedBy = 'DOCTOR' ORDER BY r.uploadedAt DESC")
    List<MedicalReport> findDoctorReportsByAppointment(@Param("appointmentId") Long appointmentId);
}