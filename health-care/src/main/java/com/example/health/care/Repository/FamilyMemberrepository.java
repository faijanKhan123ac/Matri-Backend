package com.example.health.care.Repository;

import com.example.health.care.Entity.FamilyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface FamilyMemberrepository extends JpaRepository<FamilyMember, Long> {

    List<FamilyMember> findByUserId(Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM FamilyMember f WHERE f.id = :id AND f.userId = :userId")
    void deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}