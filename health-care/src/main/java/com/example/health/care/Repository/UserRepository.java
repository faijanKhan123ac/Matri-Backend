package com.example.health.care.Repository;

import com.example.health.care.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Email se user dhundho — login/signup ke liye
    Optional<User> findByEmail(String email);
}
