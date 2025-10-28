package com.kopi.kopi.service;

import com.kopi.kopi.dto.DashboardSummary;
import com.kopi.kopi.dto.RevenuePoint;
import java.time.LocalDate;
import java.util.List;

public interface ReportService {
    enum Granularity { daily, weekly, monthly, quarterly, yearly }
    List<RevenuePoint> revenue(Granularity g, LocalDate from, LocalDate to, int buckets);
    byte[] exportRevenueToExcel(Granularity g, LocalDate from, LocalDate to, int buckets);
    DashboardSummary summary();
}
