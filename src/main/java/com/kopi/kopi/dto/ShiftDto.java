package com.kopi.kopi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftDto {
    private Integer shiftId;
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
    private String description;
    private Boolean active;
    private List<PositionRuleDto> positionRules;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionRuleDto {
        private Integer positionId;
        private boolean allowed;
        private Integer requiredCount;
    }
}
