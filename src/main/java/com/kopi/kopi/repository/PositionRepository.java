package com.kopi.kopi.repository;

import com.kopi.kopi.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Integer> {
    // return only active positions
    List<Position> findByIsActiveTrue();
}
