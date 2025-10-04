package com.kopi.kopi.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kopi.kopi.entity.User;

public interface UserRepository extends JpaRepository<User, Integer> {
	Optional<User> findByUsername(String username);
	Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    long deleteByEmailVerifiedIsFalseAndCreatedAtBefore(LocalDateTime before);
} 