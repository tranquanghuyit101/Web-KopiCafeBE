package com.kopi.kopi.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RevenuePoint {
    private String label;
    private Integer year;
    private Integer month;
    private Integer week;
    private Integer quarter;

    private BigDecimal total_sum;
    private Integer orderCount;
    private BigDecimal avgOrderValue;
    private String startDate;
    private String endDate;
}
