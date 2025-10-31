package com.kopi.kopi.service.impl;

import com.kopi.kopi.dto.DashboardSummary;
import com.kopi.kopi.dto.RevenuePoint;
import com.kopi.kopi.repository.PaymentRepository;
import com.kopi.kopi.service.ReportService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static com.kopi.kopi.service.ReportService.Granularity.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private EntityManager em;
    @Mock
    private Query nativeQuery;

    @InjectMocks
    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        // EntityManager native query is reused across calls
        when(em.createNativeQuery(anyString())).thenReturn(nativeQuery);
    }

    @AfterEach
    void tearDown() {
        // no-op
    }

    // ===================== revenue =====================

    @Test
    void should_MapDailyRevenue_WithAvg_Label_AndDates() {
        // Given
        LocalDate d = LocalDate.of(2025, 1, 10);
        BigDecimal total = new BigDecimal("100.00");
        int orders = 4;
        // paymentRepository.sumPaidByDay(fromDt, toDt) returns List<Object[]>
        when(paymentRepository.sumPaidByDay(any(), any()))
                .thenReturn((List<Object[]>) (List) List.of(new Object[] { d, total, orders }));

        // When
        var points = service.revenue(daily, d, d, 0);

        // Then
        assertThat(points).hasSize(1);
        RevenuePoint p = points.get(0);
        assertThat(p.getTotal_sum()).isEqualByComparingTo("100.00");
        assertThat(p.getOrderCount()).isEqualTo(4);
        assertThat(p.getAvgOrderValue()).isEqualByComparingTo("25.00"); // 100 / 4
        assertThat(p.getLabel()).isEqualTo("10/1");
        assertThat(p.getYear()).isEqualTo(2025);
        assertThat(p.getMonth()).isEqualTo(1);
        assertThat(p.getStartDate()).isEqualTo("2025-01-10");
        assertThat(p.getEndDate()).isEqualTo("2025-01-10");
    }

    @Test
    void should_MapWeeklyRevenue_WithTimestamp_LabelAndEndDatePlus6() {
        // Given: Monday 2025-02-03 as bucket start (ISO week)
        Timestamp ts = Timestamp.valueOf(LocalDateTime.of(2025, 2, 3, 0, 0));
        when(paymentRepository.sumPaidByWeek(any(), any()))
                .thenReturn((List<Object[]>) (List) List.of(new Object[] { ts, new BigDecimal("200"), 2 }));

        // When
        var points = service.revenue(weekly, LocalDate.of(2025, 2, 3), LocalDate.of(2025, 2, 3), 0);

        // Then
        assertThat(points).hasSize(1);
        RevenuePoint p = points.get(0);
        assertThat(p.getLabel()).startsWith("Wk ");
        assertThat(p.getStartDate()).isEqualTo("2025-02-03");
        assertThat(p.getEndDate()).isEqualTo("2025-02-09"); // +6 days
        assertThat(p.getWeek()).isNotNull();
        assertThat(p.getYear()).isNotNull();
    }

    @Test
    void should_TrimToBuckets_ForMonthly() {
        // Given 3 months, buckets=2 => keep last 2
        var jan = new Object[] { LocalDate.of(2025, 1, 1), new BigDecimal("10"), 1 };
        var feb = new Object[] { LocalDate.of(2025, 2, 1), new BigDecimal("20"), 2 };
        var mar = new Object[] { LocalDate.of(2025, 3, 1), new BigDecimal("30"), 3 };
        when(paymentRepository.sumPaidByMonth(any(), any()))
                .thenReturn((List<Object[]>) (List) List.of(jan, feb, mar));

        // When
        var points = service.revenue(monthly, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31), 2);

        // Then
        assertThat(points).hasSize(2);
        assertThat(points.get(0).getLabel()).contains("Feb");
        assertThat(points.get(1).getLabel()).contains("Mar");
    }

    @Test
    void should_MapQuarterly_EndDateAndLabel() {
        // Given: start 2025-01-01 -> end 2025-03-31, label Q1 2025
        when(paymentRepository.sumPaidByQuarter(any(), any()))
                .thenReturn((List<Object[]>) (List) List
                        .of(new Object[] { LocalDate.of(2025, 1, 1), new BigDecimal("90"), 3 }));

        // When
        var points = service.revenue(quarterly, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 3, 31), 0);

        // Then
        assertThat(points).hasSize(1);
        RevenuePoint p = points.get(0);
        assertThat(p.getEndDate()).isEqualTo("2025-03-31");
        assertThat(p.getLabel()).isEqualTo("Q1 2025");
        assertThat(p.getQuarter()).isEqualTo(1);
    }

    // ===================== exportRevenueToExcel =====================

    @Test
    void should_ExportExcel_WithMetaAndRows() throws Exception {
        // Given: spy to stub revenue points
        ReportServiceImpl spyService = Mockito.spy(service);
        List<RevenuePoint> pts = List.of(
                RevenuePoint.builder().label("Jan 2025").year(2025).month(1).total_sum(new BigDecimal("10")).build(),
                RevenuePoint.builder().label("Feb 2025").year(2025).month(2).total_sum(new BigDecimal("20")).build());
        doReturn(pts).when(spyService).revenue(eq(monthly), any(), any(), anyInt());

        // When
        byte[] bytes = spyService.exportRevenueToExcel(monthly, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 28), 2);

        // Then
        assertThat(bytes).isNotEmpty();

        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = wb.getSheet("Revenue");
            assertThat(sheet).isNotNull();

            // meta header at row 0, meta values at row 1, blank row 2
            var metaH = sheet.getRow(0);
            var metaV = sheet.getRow(1);
            assertThat(metaH.getCell(0).getStringCellValue()).isEqualTo("Granularity");
            assertThat(metaV.getCell(0).getStringCellValue()).isEqualTo("monthly");

            // table header at row 3; data starts at row 4
            var h = sheet.getRow(3);
            assertThat(h.getCell(0).getStringCellValue()).isEqualTo("Period");
            var r4 = sheet.getRow(4);
            var r5 = sheet.getRow(5);
            assertThat(r4.getCell(0).getStringCellValue()).isEqualTo("Jan 2025");
            assertThat(r4.getCell(5).getNumericCellValue()).isEqualTo(10d);
            assertThat(r5.getCell(0).getStringCellValue()).isEqualTo("Feb 2025");
            assertThat(r5.getCell(5).getNumericCellValue()).isEqualTo(20d);
        }
    }

    @Test
    void should_ExportExcel_NonEmptyBytes_AndDataRowCountMatches() throws Exception {
        // Given
        ReportServiceImpl spyService = Mockito.spy(service);
        List<RevenuePoint> pts = List.of(
                RevenuePoint.builder().label("Q1 2025").year(2025).quarter(1).total_sum(new BigDecimal("90")).build());
        doReturn(pts).when(spyService).revenue(eq(quarterly), any(), any(), anyInt());

        // When
        byte[] bytes = spyService.exportRevenueToExcel(quarterly, null, null, 1);

        // Then
        assertThat(bytes).isNotEmpty();
        try (var wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = wb.getSheet("Revenue");
            assertThat(sheet.getPhysicalNumberOfRows()).isGreaterThanOrEqualTo(5); // 4 header/meta rows + 1 data row
        }
    }

    // ===================== summary =====================

    @Test
    void should_BuildSummary_FromRepositoryAndCounts() {
        // Given: sums and counts (order: today, week, month, year)
        when(paymentRepository.sumPaidBetween(any(), any()))
                .thenReturn(new BigDecimal("10.5")) // today
                .thenReturn(new BigDecimal("21.0")) // week
                .thenReturn(new BigDecimal("31.0")) // month
                .thenReturn(new BigDecimal("41.0"));// year
        when(paymentRepository.countPaidBetween(any(), any()))
                .thenReturn(1) // today
                .thenReturn(2) // week
                .thenReturn(3) // month
                .thenReturn(4);// year

        // 3 native counts in order: products, promos, pendingOrders
        when(nativeQuery.getSingleResult())
                .thenReturn(100) // products
                .thenReturn(3) // promos
                .thenReturn(7); // pending

        // When
        DashboardSummary s = service.summary();

        // Then
        assertThat(s.getTodayRevenue()).isEqualByComparingTo("10.5");
        assertThat(s.getWeekRevenue()).isEqualByComparingTo("21.0");
        assertThat(s.getMonthRevenue()).isEqualByComparingTo("31.0");
        assertThat(s.getYearRevenue()).isEqualByComparingTo("41.0");
        assertThat(s.getTodayOrders()).isEqualTo(1);
        assertThat(s.getWeekOrders()).isEqualTo(2);
        assertThat(s.getMonthOrders()).isEqualTo(3);
        assertThat(s.getYearOrders()).isEqualTo(4);
        assertThat(s.getTotalProducts()).isEqualTo(100);
        assertThat(s.getActivePromos()).isEqualTo(3);
        assertThat(s.getPendingOrders()).isEqualTo(7);

        // Verify calls counts (not bắt buộc nhưng giúp chắc chắn đường đi)
        verify(paymentRepository, times(4)).sumPaidBetween(any(), any());
        verify(paymentRepository, times(4)).countPaidBetween(any(), any());
        verify(em, times(3)).createNativeQuery(anyString());
        verify(nativeQuery, times(3)).getSingleResult();
    }

    @Test
    void should_ReturnZeros_When_SumsNull_AndNativeErrors() {
        // Given: sums null
        when(paymentRepository.sumPaidBetween(any(), any()))
                .thenReturn(null, null, null, null);
        when(paymentRepository.countPaidBetween(any(), any()))
                .thenReturn(0, 0, 0, 0);

        // Native query throws or returns null → safeCount => 0
        when(em.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult())
                .thenReturn(null) // products
                .thenThrow(new RuntimeException("DB down")) // promos
                .thenReturn(null); // pending (won't be reached if throw breaks; but safeCount catches)
        // Note: safeCount catch-all sẽ trả 0 cho mọi vấn đề.

        // When
        DashboardSummary s = service.summary();

        // Then
        assertThat(s.getTodayRevenue()).isEqualByComparingTo("0");
        assertThat(s.getWeekRevenue()).isEqualByComparingTo("0");
        assertThat(s.getMonthRevenue()).isEqualByComparingTo("0");
        assertThat(s.getYearRevenue()).isEqualByComparingTo("0");
        assertThat(s.getTotalProducts()).isEqualTo(0);
        assertThat(s.getActivePromos()).isEqualTo(0);
        assertThat(s.getPendingOrders()).isEqualTo(0);
    }
}
