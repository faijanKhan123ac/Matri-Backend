package com.example.health.care.Repository;

import com.example.health.care.Entity.Medicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findByCategory(String category);
    @Query("SELECT m FROM Medicine m WHERE LOWER(m.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(m.brand) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(m.category) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Medicine> search(String q);
    List<Medicine> findByPregnancySafe(Boolean pregnancySafe);
}