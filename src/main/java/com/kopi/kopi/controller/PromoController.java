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
import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;

import com.kopi.kopi.service.IPromoService;

@RestController
@RequestMapping("/apiv1/promo")
public class PromoController {
    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountEventRepository discountEventRepository;
    private final ProductRepository productRepository;
    private final IPromoService promoService;

    public PromoController(DiscountCodeRepository discountCodeRepository, DiscountEventRepository discountEventRepository, ProductRepository productRepository, IPromoService promoService) {
        this.discountCodeRepository = discountCodeRepository;
        this.discountEventRepository = discountEventRepository;
        this.productRepository = productRepository;
        this.promoService = promoService;
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

    /**
     * List all discounts (codes + events) with optional filters and pagination
     * Params:
     * - page (default 1)
     * - limit (default 8)
     * - available (true|false) - when true, include only active and not ended items (includes current and upcoming)
     * - status (all|current|upcoming) - alternative to available for finer filtering
     * - searchByName (optional) - matches code or name contains (case-insensitive)
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> list(
            @RequestParam(name = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(name = "limit", required = false, defaultValue = "8") Integer limit,
            @RequestParam(name = "available", required = false) String available,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "searchByName", required = false, defaultValue = "") String searchByName
    ) {
        return promoService.list(page, limit, available, status, searchByName);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOne(@PathVariable("id") Integer id) {
        var dc = discountCodeRepository.findById(id).orElse(null);
        if (dc != null) {
            return ResponseEntity.ok(
                    Map.ofEntries(
                            Map.entry("id", dc.getDiscountCodeId()),
                            Map.entry("kind", "CODE"),
                            Map.entry("title", dc.getCode()),
                            Map.entry("couponCode", dc.getCode()),
                            Map.entry("description", dc.getDescription()),
                            Map.entry("discountType", dc.getDiscountType() != null ? dc.getDiscountType().name() : null),
                            Map.entry("discountValue", dc.getDiscountValue()),
                            Map.entry("minOrderAmount", dc.getMinOrderAmount()),
                            Map.entry("totalUsageLimit", dc.getTotalUsageLimit()),
                            Map.entry("startsAt", dc.getStartsAt()),
                            Map.entry("endsAt", dc.getEndsAt()),
                            Map.entry("active", dc.getActive())
                    )
            );
        }
        var ev = discountEventRepository.findById(id).orElse(null);
        if (ev != null) {
            java.util.List<Integer> pids = new java.util.ArrayList<>();
            java.util.List<java.util.Map<String, Object>> plist = new java.util.ArrayList<>();
            for (var dep : ev.getProducts()) {
                var p = dep.getProduct();
                if (p == null) continue;
                pids.add(p.getProductId());
                plist.add(java.util.Map.of(
                        "id", p.getProductId(),
                        "name", p.getName(),
                        "price", p.getPrice(),
                        "category_name", p.getCategory() != null ? p.getCategory().getName() : null
                ));
            }
            return ResponseEntity.ok(
                    Map.ofEntries(
                            Map.entry("id", ev.getDiscountEventId()),
                            Map.entry("kind", "EVENT"),
                            Map.entry("title", ev.getName()),
                            Map.entry("description", ev.getDescription()),
                            Map.entry("discountType", ev.getDiscountType() != null ? ev.getDiscountType().name() : null),
                            Map.entry("discountValue", ev.getDiscountValue()),
                            Map.entry("startsAt", ev.getStartsAt()),
                            Map.entry("endsAt", ev.getEndsAt()),
                            Map.entry("active", ev.getActive()),
                            Map.entry("productIds", pids),
                            Map.entry("products", plist)
                    )
            );
        }
        return ResponseEntity.notFound().build();
    }

    public record UpdatePromoPayload(
            String name,
            String desc,
            String discount_type,
            String discount_value,
            String coupon_code,
            String min_order_amount,
            String total_usage_limit,
            String start_date,
            String end_date,
            java.util.List<Integer> product_ids
    ) {}

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable("id") Integer id, @RequestBody UpdatePromoPayload body) {
        var dc = discountCodeRepository.findById(id).orElse(null);
        if (dc != null) {
            if (body.coupon_code() != null && !body.coupon_code().isBlank()) dc.setCode(body.coupon_code().trim().toUpperCase());
            if (body.desc() != null) dc.setDescription(body.desc());
            if (body.discount_type() != null) dc.setDiscountType(parseDiscountType(body.discount_type()));
            if (body.discount_value() != null) dc.setDiscountValue(parseDecimal(body.discount_value(), dc.getDiscountValue()));
            if (body.min_order_amount() != null) dc.setMinOrderAmount(parseDecimal(body.min_order_amount(), dc.getMinOrderAmount()));
            if (body.total_usage_limit() != null) dc.setTotalUsageLimit(parseInteger(body.total_usage_limit(), dc.getTotalUsageLimit()));
            if (body.start_date() != null) dc.setStartsAt(parseDateTime(body.start_date()));
            if (body.end_date() != null) dc.setEndsAt(parseDateTime(body.end_date()));
            discountCodeRepository.save(dc);
            return ResponseEntity.ok(Map.of("message", "updated"));
        }
        var ev = discountEventRepository.findById(id).orElse(null);
        if (ev != null) {
            if (body.name() != null) ev.setName(body.name());
            if (body.desc() != null) ev.setDescription(body.desc());
            if (body.discount_type() != null) ev.setDiscountType(parseDiscountType(body.discount_type()));
            if (body.discount_value() != null) ev.setDiscountValue(parseDecimal(body.discount_value(), ev.getDiscountValue()));
            if (body.start_date() != null) ev.setStartsAt(parseDateTime(body.start_date()));
            if (body.end_date() != null) ev.setEndsAt(parseDateTime(body.end_date()));
            if (body.product_ids() != null) {
                // Replace products
                ev.getProducts().clear();
                for (Integer pid : body.product_ids()) {
                    Product p = productRepository.findById(pid).orElse(null);
                    if (p == null) continue;
                    DiscountEventProduct dep = new DiscountEventProduct();
                    dep.setDiscountEvent(ev);
                    dep.setProduct(p);
                    ev.getProducts().add(dep);
                }
            }
            discountEventRepository.save(ev);
            return ResponseEntity.ok(Map.of("message", "updated"));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> softDelete(@PathVariable("id") Integer id) {
        var dc = discountCodeRepository.findById(id).orElse(null);
        if (dc != null) {
            dc.setActive(false);
            discountCodeRepository.save(dc);
            return ResponseEntity.ok(Map.of("message", "deleted"));
        }
        var ev = discountEventRepository.findById(id).orElse(null);
        if (ev != null) {
            ev.setActive(false);
            discountEventRepository.save(ev);
            return ResponseEntity.ok(Map.of("message", "deleted"));
        }
        return ResponseEntity.notFound().build();
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