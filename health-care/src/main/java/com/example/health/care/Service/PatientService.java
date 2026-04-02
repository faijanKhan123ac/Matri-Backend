package com.example.health.care.Service;

import com.example.health.care.Entity.Patient;
import com.example.health.care.Repository.PatientRepository;
import org.springframework.stereotype.Service;

@Service
public class PatientService {

    private final PatientRepository patientRepository;

    public PatientService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    /**
     * Naya patient register karo
     */
    public Patient registerPatient(Patient patient) {
        // Ek user sirf ek baar patient register kar sakta hai
        Long userId = patient.getUser().getId();
        if (patientRepository.existsByUserId(userId)) {
            throw new RuntimeException("Aap pehle se patient registered hain.");
        }
        return patientRepository.save(patient);
    }

    /**
     * Patient ki GPS location update karo (real-time tracking ke liye)
     *
     * @param patientId Patient ka ID
     * @param latitude  Nayi latitude
     * @param longitude Nayi longitude
     * @return Updated patient
     */
    public Patient updateLocation(Long patientId, double latitude, double longitude) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient nahi mila with ID: " + patientId));

        patient.setLatitude(latitude);
        patient.setLongitude(longitude);

        return patientRepository.save(patient);
    }

    /**
     * Patient ki details ID se fetch karo
     */
    public Patient getPatientById(Long patientId) {
        return patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient nahi mila with ID: " + patientId));
    }

    /**
     * User ID se patient fetch karo — login ke baad auto-fill ke liye
     */
    public Patient getPatientByUserId(Long userId) {
        return patientRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Is user ka patient profile nahi mila."));
    }
}