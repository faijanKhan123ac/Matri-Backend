package com.example.health.care.Repository;

import com.example.health.care.Entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByDoctorId(Long doctorId);

    // Check 1: Same doctor, same date, same time pe koi active booking hai?
    @Query("SELECT COUNT(a) > 0 FROM Appointment a " +
            "WHERE a.doctorId = :doctorId " +
            "AND a.date = :date " +
            "AND a.time = :time " +
            "AND a.status <> com.example.health.care.Entity.Appointment.AppointmentStatus.CANCELLED")
    boolean existsActiveBooking(
            @Param("doctorId") Long doctorId,
            @Param("date") LocalDate date,
            @Param("time") LocalTime time);

    // Check 2: Same patient ne same doctor ko same date pe already book kiya?
    boolean existsByPatientIdAndDoctorIdAndDate(
            Long patientId, Long doctorId, LocalDate date);

    // Reminder: Kal ki saari active appointments fetch karo
    @Query("SELECT a FROM Appointment a " +
            "WHERE a.date = :tomorrow " +
            "AND a.status <> com.example.health.care.Entity.Appointment.AppointmentStatus.CANCELLED")
    List<Appointment> findTomorrowAppointments(@Param("tomorrow") LocalDate tomorrow);
}