package com.kopi.kopi.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {
    private BigDecimal todayRevenue;
    private Integer todayOrders;

    private BigDecimal weekRevenue;
    private Integer weekOrders;

    private BigDecimal monthRevenue;
    private Integer monthOrders;

    private BigDecimal yearRevenue;
    private Integer yearOrders;

    private Integer totalProducts;   // nếu chưa có bảng -> sẽ trả 0 an toàn
    private Integer activePromos;    // nếu chưa có bảng -> sẽ trả 0 an toàn
    private Integer pendingOrders;   // tạm tính theo payments.status='pending'
}
