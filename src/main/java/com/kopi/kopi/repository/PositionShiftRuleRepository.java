package com.kopi.kopi.repository;

import com.kopi.kopi.entity.PositionShiftRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionShiftRuleRepository extends JpaRepository<PositionShiftRule, Integer> {
    List<PositionShiftRule> findByShiftShiftId(Integer shiftId);

    List<PositionShiftRule> findByPositionPositionId(Integer positionId);
}
