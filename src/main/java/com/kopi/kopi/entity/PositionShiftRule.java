package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "position_shift_rules", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionShiftRule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Integer ruleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    @ToString.Exclude
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id", nullable = false)
    @ToString.Exclude
    private Shift shift;

    @Column(name = "is_allowed", nullable = false)
    private boolean isAllowed;

    @Column(name = "required_count")
    private Integer requiredCount;
}
