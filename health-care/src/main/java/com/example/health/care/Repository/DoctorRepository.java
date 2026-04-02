package com.example.health.care.Repository;

import com.example.health.care.Entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUserId(Long userId);

    List<Doctor> findByLatitudeBetweenAndLongitudeBetween(
            Double minLat, Double maxLat, Double minLng, Double maxLng);

    List<Doctor> findBySpecializationIgnoreCase(String specialization);
}