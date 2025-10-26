package com.kopi.kopi.controller;

import com.kopi.kopi.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/apiv1/adminPanel")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRevenue(
            @RequestParam(name = "view", defaultValue = "monthly") String view,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to",   required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "buckets", defaultValue = "7") int buckets) {
        try {
            var g = parseView(view);
            var data = reportService.revenue(g, from, to, buckets);

            // ❌ Map.of không chấp nhận null => dùng Map thường + put có điều kiện
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("view", g.name());
            if (from != null) meta.put("from", from);
            if (to   != null) meta.put("to", to);
            meta.put("count", data != null ? data.size() : 0);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("data", data);
            body.put("meta", meta);

            return ResponseEntity.ok(body);
        } catch (Exception ex) {
            ex.printStackTrace();
            String detail = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "" : ex.getMessage());
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("message", "Lỗi hệ thống");
            err.put("detail", detail);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
        }
    }

    @GetMapping("/reports/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> export(
            @RequestParam(name = "view", defaultValue = "monthly") String view,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to",   required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "buckets", defaultValue = "7") int buckets) {

        var g = parseView(view);
        byte[] bytes = reportService.exportRevenueToExcel(g, from, to, buckets);

        String fn = "revenue_" + g.name() + "_" + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ".xlsx";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", fn);
        headers.setContentLength(bytes.length);
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    // Parse view an toàn: không phân biệt hoa/thường + alias
    private ReportService.Granularity parseView(String v) {
        if (v == null || v.isBlank()) return ReportService.Granularity.monthly;
        String s = v.trim();
        try { return ReportService.Granularity.valueOf(s); } catch (Exception ignore) {}
        try { return ReportService.Granularity.valueOf(s.toUpperCase()); } catch (Exception ignore) {}
        try { return ReportService.Granularity.valueOf(s.toLowerCase()); } catch (Exception ignore) {}
        return switch (s.toLowerCase()) {
            case "day","d","daily"          -> ReportService.Granularity.daily;
            case "week","w","weekly"        -> ReportService.Granularity.weekly;
            case "month","m","monthly"      -> ReportService.Granularity.monthly;
            case "quarter","q","quarterly"  -> ReportService.Granularity.quarterly;
            case "year","y","yearly"        -> ReportService.Granularity.yearly;
            default                          -> ReportService.Granularity.monthly;
        };
    }

    @GetMapping("/reports/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getSummary() {
        try {
            var sum = reportService.summary();
            return ResponseEntity.ok(sum);
        } catch (Exception ex) {
            ex.printStackTrace();
            String detail = ex.getClass().getSimpleName() + ": " + (ex.getMessage()==null?"":ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message","Lỗi hệ thống","detail", detail));
        }
    }

}
