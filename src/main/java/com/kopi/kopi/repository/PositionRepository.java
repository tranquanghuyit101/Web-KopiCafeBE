package com.kopi.kopi.repository;

import com.kopi.kopi.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionRepository extends JpaRepository<Position, Integer> {
    // return only active positions
    java.util.List<Position> findByIsActiveTrue();
}
