package com.example.health.care.Repository;

import com.example.health.care.Entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUserId(Long userId);

    // Ek user ne pehle se patient register kiya hai ya nahi
    boolean existsByUserId(Long userId);
}