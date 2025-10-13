package com.kopi.kopi.controller;

import com.kopi.kopi.entity.DiscountCode;
import com.kopi.kopi.entity.DiscountEvent;
import com.kopi.kopi.entity.DiscountEventProduct;
import com.kopi.kopi.entity.Product;
import com.kopi.kopi.entity.enums.DiscountType;
import com.kopi.kopi.repository.DiscountCodeRepository;
import com.kopi.kopi.repository.DiscountEventRepository;
import com.kopi.kopi.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/apiv1/promo")
public class PromoController {
    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountEventRepository discountEventRepository;
    private final ProductRepository productRepository;

    public PromoController(DiscountCodeRepository discountCodeRepository, DiscountEventRepository discountEventRepository, ProductRepository productRepository) {
        this.discountCodeRepository = discountCodeRepository;
        this.discountEventRepository = discountEventRepository;
        this.productRepository = productRepository;
    }

    public record CreateCodePayload(
            String coupon_code,
            String discount_type,
            String discount_value,
            String discount,
            String min_order_amount,
            String total_usage_limit,
            String name,
            String desc,
            String start_date,
            String end_date
    ) {}

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody CreateCodePayload body) {
        if (body == null || body.coupon_code() == null || body.coupon_code().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "coupon_code is required"));
        }

        // Duplicate code check
        if (discountCodeRepository.findByCodeIgnoreCase(body.coupon_code().trim()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("message", "coupon_code already exists"));
        }

        DiscountCode dc = new DiscountCode();
        dc.setCode(body.coupon_code().trim().toUpperCase());
        dc.setDescription(body.desc());
        // Accept either (discount_type + discount_value) or legacy single 'discount' (percent)
        DiscountType type = parseDiscountType(body.discount_type());
        BigDecimal value = parseDecimal(body.discount_value(), null);
        if (value == null && body.discount() != null && !body.discount().isBlank()) {
            type = DiscountType.PERCENT;
            value = parseDecimal(body.discount(), BigDecimal.ZERO);
        }
        dc.setDiscountType(type);
        dc.setDiscountValue(value != null ? value : BigDecimal.ZERO);
        dc.setMinOrderAmount(parseDecimal(body.min_order_amount(), null));
        dc.setStartsAt(parseDateTime(body.start_date()));
        dc.setEndsAt(parseDateTime(body.end_date()));
        dc.setTotalUsageLimit(parseInteger(body.total_usage_limit(), null));
        dc.setPerUserLimit(null);
        dc.setActive(true);
        dc.setUsageCount(0);
        dc.setCreatedAt(LocalDateTime.now());

        discountCodeRepository.save(dc);
        return ResponseEntity.ok(Map.of("message", "created"));
    }

    public record CreateEventPayload(
            String name,
            String desc,
            String discount_type,
            String discount_value,
            String start_date,
            String end_date,
            List<Integer> product_ids
    ) {}

    @PostMapping("/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createEvent(@RequestBody CreateEventPayload body) {
        if (body == null || body.name() == null || body.name().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "name is required"));
        }
        if (body.product_ids() == null || body.product_ids().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "product_ids required"));
        }

        DiscountEvent ev = new DiscountEvent();
        ev.setName(body.name());
        ev.setDescription(body.desc());
        ev.setDiscountType(parseDiscountType(body.discount_type()));
        ev.setDiscountValue(parseDecimal(body.discount_value(), BigDecimal.ZERO));
        ev.setStartsAt(parseDateTime(body.start_date()));
        ev.setEndsAt(parseDateTime(body.end_date()));
        ev.setActive(true);
        ev.setCreatedAt(LocalDateTime.now());

        // attach products
        for (Integer pid : body.product_ids()) {
            Product p = productRepository.findById(pid).orElse(null);
            if (p == null) continue;
            DiscountEventProduct dep = new DiscountEventProduct();
            dep.setDiscountEvent(ev);
            dep.setProduct(p);
            ev.getProducts().add(dep);
        }

        discountEventRepository.save(ev);
        return ResponseEntity.ok(Map.of("message", "created"));
    }

    private DiscountType parseDiscountType(String t) {
        if (t == null) return DiscountType.AMOUNT;
        return t.equalsIgnoreCase("PERCENT") ? DiscountType.PERCENT : DiscountType.AMOUNT;
    }

    private BigDecimal parseDecimal(String s, BigDecimal def) {
        try { return s == null || s.isBlank() ? def : new BigDecimal(s); } catch (Exception e) { return def; }
    }

    private Integer parseInteger(String s, Integer def) {
        try { return s == null || s.isBlank() ? def : Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        String raw = s.replace("\"", "").trim();
        // Try multiple formats: OffsetDateTime, Instant, LocalDateTime, LocalDate
        try {
            java.time.OffsetDateTime odt = java.time.OffsetDateTime.parse(raw);
            return odt.toLocalDateTime();
        } catch (Exception ignored) {}
        try {
            java.time.Instant inst = java.time.Instant.parse(raw);
            return java.time.LocalDateTime.ofInstant(inst, java.time.ZoneId.systemDefault());
        } catch (Exception ignored) {}
        try {
            return java.time.LocalDateTime.parse(raw, java.time.format.DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ignored) {}
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(raw, java.time.format.DateTimeFormatter.ISO_DATE);
            return d.atStartOfDay();
        } catch (Exception ignored) {}
        return null;
    }
}


