package com.example.health.care.Service;

import com.example.health.care.Entity.Doctor;
import com.example.health.care.Repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DoctorService {

    @Autowired
    private DoctorRepository doctorRepository;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DEFAULT_RADIUS_KM = 10.0;

    public Doctor registerDoctor(Doctor doctor) {
        if (doctor.getUserId() != null && doctorRepository.existsByUserId(doctor.getUserId()))
            throw new RuntimeException("Aap pehle se doctor registered hain.");
        if (doctor.getEmail() != null && !doctor.getEmail().isBlank())
            if (doctorRepository.existsByEmail(doctor.getEmail()))
                throw new RuntimeException("Yeh email already registered hai.");
        // Plain text password — no encoding
        return doctorRepository.save(doctor);
    }

    public Doctor loginDoctor(String email, String password) {
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email registered nahi hai."));
        if (doctor.getPassword() == null)
            throw new RuntimeException("Password set nahi hai. Reset karo.");
        if (!doctor.getPassword().equals(password))
            throw new RuntimeException("Password galat hai.");
        return doctor;
    }

    public Doctor resetPassword(String email, String newPassword) {
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email registered nahi hai."));
        doctor.setPassword(newPassword);
        return doctorRepository.save(doctor);
    }

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public Doctor getDoctorById(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Doctor nahi mila ID: " + id));
    }

    public Doctor updateLocation(Long id, Double latitude, Double longitude) {
        Doctor doctor = getDoctorById(id);
        doctor.setLatitude(latitude);
        doctor.setLongitude(longitude);
        return doctorRepository.save(doctor);
    }

    public List<Doctor> getNearbyDoctors(Double userLat, Double userLng, Double radiusKm) {
        if (radiusKm == null) radiusKm = DEFAULT_RADIUS_KM;
        double latDelta = Math.toDegrees(radiusKm / EARTH_RADIUS_KM);
        double lngDelta = Math.toDegrees(radiusKm /
                (EARTH_RADIUS_KM * Math.cos(Math.toRadians(userLat))));
        List<Doctor> candidates = doctorRepository.findByLatitudeBetweenAndLongitudeBetween(
                userLat - latDelta, userLat + latDelta,
                userLng - lngDelta, userLng + lngDelta);
        List<Doctor> result = new ArrayList<>();
        for (Doctor d : candidates) {
            if (d.getLatitude() != null && d.getLongitude() != null) {
                double dist = haversine(userLat, userLng, d.getLatitude(), d.getLongitude());
                if (dist <= radiusKm) result.add(d);
            }
        }
        result.sort((a, b) -> Double.compare(
                haversine(userLat, userLng, a.getLatitude(), a.getLongitude()),
                haversine(userLat, userLng, b.getLatitude(), b.getLongitude())));
        return result;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon/2)*Math.sin(dLon/2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}