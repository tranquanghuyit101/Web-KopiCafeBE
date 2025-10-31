package com.kopi.kopi.controller;

import com.kopi.kopi.dto.RevenuePoint;
import com.kopi.kopi.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminReportPageController {

    private final ReportService reportService;

    @GetMapping("/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public String revenuePage(
            @RequestParam(name = "view", defaultValue = "monthly") String view,
            @RequestParam(name = "from", required = false) LocalDate from,
            @RequestParam(name = "to", required = false) LocalDate to,
            @RequestParam(name = "buckets", defaultValue = "12") int buckets,
            Model model) {
        var g = ReportService.Granularity.valueOf(view);
        List<RevenuePoint> points = reportService.revenue(g, from, to, buckets);
        model.addAttribute("points", points);
        model.addAttribute("granularity", g.name().toLowerCase());
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("buckets", buckets);
        return "admin/reports/revenue";
    }
}
