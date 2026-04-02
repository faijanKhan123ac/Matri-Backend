package com.example.health.care.Repository;

import com.example.health.care.Entity.MedicineOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MedicineOrderRepository extends JpaRepository<MedicineOrder, Long> {
    List<MedicineOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
}