package com.kopi.kopi.repository;

import com.kopi.kopi.entity.Shift;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShiftRepository extends JpaRepository<Shift, Integer> {
    List<Shift> findByIsActiveTrue();
}
