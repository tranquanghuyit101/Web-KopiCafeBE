package com.kopi.kopi.service.impl;

import com.kopi.kopi.dto.DashboardSummary;
import com.kopi.kopi.dto.RevenuePoint;
import com.kopi.kopi.repository.PaymentRepository;
import com.kopi.kopi.service.ReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.*;
import java.math.RoundingMode;
import java.time.temporal.TemporalAdjusters;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final PaymentRepository paymentRepository;

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<RevenuePoint> revenue(Granularity g, LocalDate from, LocalDate to, int buckets) {
        if (to == null) to = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        if (from == null) {
            switch (g) {
                case daily     -> from = to.minusDays(6);
                case weekly    -> from = to.minusWeeks(6);
                case monthly   -> from = to.minusMonths(6);
                case quarterly -> from = to.minusMonths(18); // 6 quý
                case yearly    -> from = to.minusYears(6);
            }
        }
        var fromDt = from.atStartOfDay();
        var toDt   = to.plusDays(1).atStartOfDay().minusNanos(1);

        List<Object[]> raw = switch (g) {
            case daily     -> paymentRepository.sumPaidByDay(fromDt, toDt);
            case weekly    -> paymentRepository.sumPaidByWeek(fromDt, toDt);
            case monthly   -> paymentRepository.sumPaidByMonth(fromDt, toDt);
            case quarterly -> paymentRepository.sumPaidByQuarter(fromDt, toDt);
            case yearly    -> paymentRepository.sumPaidByYear(fromDt, toDt);
        };

        List<RevenuePoint> list = new ArrayList<>();
        for (Object[] row : raw) {
            // ---- bucketStart: an toàn cho mọi kiểu date/datetime
            Object dtObj = row[0];
            LocalDate bucketStart;
            if (dtObj instanceof java.sql.Date d)       bucketStart = d.toLocalDate();
            else if (dtObj instanceof java.sql.Timestamp ts) bucketStart = ts.toLocalDateTime().toLocalDate();
            else if (dtObj instanceof LocalDate ld)     bucketStart = ld;
            else if (dtObj instanceof LocalDateTime ldt)bucketStart = ldt.toLocalDate();
            else {
                String s = String.valueOf(dtObj);
                bucketStart = LocalDate.parse(s.substring(0, 10));
            }

            BigDecimal total = toBigDecimal(row[1]);
            int orderCount = 0;
            if (row.length >= 3 && row[2] != null) orderCount = ((Number) row[2]).intValue();

            BigDecimal avg = BigDecimal.ZERO;
            if (orderCount > 0 && total != null)
                avg = total.divide(BigDecimal.valueOf(orderCount), 2, RoundingMode.HALF_UP);

            // tính date range theo granularity
            LocalDate start = bucketStart;
            LocalDate end;
            switch (g) {
                case daily     -> end = start;
                case weekly    -> end = start.plusDays(6);
                case monthly   -> end = start.with(TemporalAdjusters.lastDayOfMonth());
                case quarterly -> end = start.plusMonths(3).minusDays(1);
                case yearly    -> end = start.with(TemporalAdjusters.lastDayOfYear());
                default        -> end = start;
            }

            var b = RevenuePoint.builder()
                    .total_sum(total)
                    .orderCount(orderCount)
                    .avgOrderValue(avg)
                    .startDate(start.toString())
                    .endDate(end.toString());

            // label + meta
            switch (g) {
                case daily -> {
                    b.label(start.getDayOfMonth() + "/" + start.getMonthValue())
                            .year(start.getYear()).month(start.getMonthValue());
                }
                case weekly -> {
                    WeekFields wf = WeekFields.ISO;
                    int week = start.get(wf.weekOfWeekBasedYear());
                    b.label("Wk " + week).year(start.get(wf.weekBasedYear())).week(week);
                }
                case monthly -> {
                    String m = start.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    b.label(m + " " + start.getYear())
                            .year(start.getYear()).month(start.getMonthValue());
                }
                case quarterly -> {
                    int q = ((start.getMonthValue()-1)/3) + 1;
                    b.label("Q" + q + " " + start.getYear())
                            .year(start.getYear()).quarter(q);
                }
                case yearly -> b.label(Integer.toString(start.getYear())).year(start.getYear());
            }

            list.add(b.build());
        }

        if (buckets > 0 && list.size() > buckets) {
            list = list.subList(list.size() - buckets, list.size());
        }
        return list;
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal bd) return bd;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(v.toString());
    }

    @Override
    public byte[] exportRevenueToExcel(Granularity g, LocalDate from, LocalDate to, int buckets) {
        var points = revenue(g, from, to, buckets);
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet("Revenue");
            int r = 0;
            var metaH = sheet.createRow(r++);
            metaH.createCell(0).setCellValue("Granularity");
            metaH.createCell(1).setCellValue("From");
            metaH.createCell(2).setCellValue("To");
            metaH.createCell(3).setCellValue("Buckets");
            metaH.createCell(4).setCellValue("GeneratedAt");

            var metaV = sheet.createRow(r++);
            metaV.createCell(0).setCellValue(g.name());
            metaV.createCell(1).setCellValue(from != null ? from.toString() : "");
            metaV.createCell(2).setCellValue(to   != null ? to.toString()   : "");
            metaV.createCell(3).setCellValue(points.size());
            metaV.createCell(4).setCellValue(OffsetDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString());

            r++;
            var h = sheet.createRow(r++);
            h.createCell(0).setCellValue("Period");
            h.createCell(1).setCellValue("Year");
            h.createCell(2).setCellValue("Month");
            h.createCell(3).setCellValue("Week");
            h.createCell(4).setCellValue("Quarter");
            h.createCell(5).setCellValue("Total");

            for (var p : points) {
                var row = sheet.createRow(r++);
                row.createCell(0).setCellValue(p.getLabel());
                if (p.getYear()    != null) row.createCell(1).setCellValue(p.getYear());
                if (p.getMonth()   != null) row.createCell(2).setCellValue(p.getMonth());
                if (p.getWeek()    != null) row.createCell(3).setCellValue(p.getWeek());
                if (p.getQuarter() != null) row.createCell(4).setCellValue(p.getQuarter());
                row.createCell(5).setCellValue(p.getTotal_sum() != null ? p.getTotal_sum().doubleValue() : 0d);
            }
            for (int c = 0; c <= 5; c++) sheet.autoSizeColumn(c);
            var bos = new java.io.ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export Excel", e);
        }
    }

    @Override
    public DashboardSummary summary() {
        var zone = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
        var today = java.time.LocalDate.now(zone);

        var startToday = today.atStartOfDay();
        var endToday   = today.plusDays(1).atStartOfDay().minusNanos(1);

        var wf = WeekFields.ISO;
        var weekStart = today.with(wf.dayOfWeek(), 1).atStartOfDay();
        var weekEnd   = today.plusDays(1).atStartOfDay().minusNanos(1); // tới cuối hôm nay

        var monthStart = today.withDayOfMonth(1).atStartOfDay();
        var yearStart  = today.withDayOfYear(1).atStartOfDay();
        var endNow     = endToday;

        BigDecimal todayRev  = nz(paymentRepository.sumPaidBetween(startToday, endToday));
        int        todayCnt  = paymentRepository.countPaidBetween(startToday, endToday);

        BigDecimal weekRev   = nz(paymentRepository.sumPaidBetween(weekStart, endNow));
        int        weekCnt   = paymentRepository.countPaidBetween(weekStart, endNow);

        BigDecimal monthRev  = nz(paymentRepository.sumPaidBetween(monthStart, endNow));
        int        monthCnt  = paymentRepository.countPaidBetween(monthStart, endNow);

        BigDecimal yearRev   = nz(paymentRepository.sumPaidBetween(yearStart, endNow));
        int        yearCnt   = paymentRepository.countPaidBetween(yearStart, endNow);

        int totalProducts = safeCount("""
            SELECT COUNT(1) FROM dbo.products
            WHERE (is_available = 1 OR is_available IS NULL)
        """);

        int activePromos = safeCount("""
            SELECT COUNT(1) FROM dbo.discount_events
            WHERE is_active = 1
              AND (starts_at IS NULL OR starts_at <= SYSDATETIME())
              AND (ends_at   IS NULL OR ends_at   >= SYSDATETIME())
        """);

        int pendingOrders = safeCount("""
            SELECT COUNT(1) FROM dbo.orders WHERE status = 'pending'
        """);

        return DashboardSummary.builder()
                .todayRevenue(todayRev).todayOrders(todayCnt)
                .weekRevenue(weekRev).weekOrders(weekCnt)
                .monthRevenue(monthRev).monthOrders(monthCnt)
                .yearRevenue(yearRev).yearOrders(yearCnt)
                .totalProducts(totalProducts)
                .activePromos(activePromos)
                .pendingOrders(pendingOrders)
                .build();
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }

    private int safeCount(String sql) {
        try {
            Object x = em.createNativeQuery(sql).getSingleResult();
            if (x instanceof Number n) return n.intValue();
        } catch (Exception ignored) {}
        return 0;
    }
}

