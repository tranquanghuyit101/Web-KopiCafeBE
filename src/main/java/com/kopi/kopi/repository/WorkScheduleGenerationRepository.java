package com.kopi.kopi.repository;

import com.kopi.kopi.entity.WorkScheduleGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkScheduleGenerationRepository extends JpaRepository<WorkScheduleGeneration, Integer> {
    List<WorkScheduleGeneration> findAllByOrderByCreatedAtDesc();
}
