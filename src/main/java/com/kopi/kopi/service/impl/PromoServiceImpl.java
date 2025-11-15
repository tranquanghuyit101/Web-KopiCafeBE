package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.DiscountCode;
import com.kopi.kopi.entity.DiscountEvent;
import com.kopi.kopi.entity.DiscountEventProduct;
import com.kopi.kopi.entity.Product;
import com.kopi.kopi.entity.enums.DiscountType;
import com.kopi.kopi.repository.DiscountCodeRepository;
import com.kopi.kopi.repository.DiscountEventRepository;
import com.kopi.kopi.repository.ProductRepository;
import com.kopi.kopi.service.IPromoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.kopi.kopi.dto.promo.CreateCodeDTO;
import com.kopi.kopi.dto.promo.CreateEventDTO;
import com.kopi.kopi.dto.promo.UpdatePromoDTO;
import com.kopi.kopi.dto.promo.PromoDetailDTO;
import com.kopi.kopi.dto.promo.PromoProductDTO;

@Service
public class PromoServiceImpl implements IPromoService {
    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountEventRepository discountEventRepository;
    private final ProductRepository productRepository;

    public PromoServiceImpl(DiscountCodeRepository discountCodeRepository, DiscountEventRepository discountEventRepository, ProductRepository productRepository) {
        this.discountCodeRepository = discountCodeRepository;
        this.discountEventRepository = discountEventRepository;
        this.productRepository = productRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> list(Integer page, Integer limit, String available, String status, String searchByName) {
        LocalDateTime now = LocalDateTime.now();
        boolean hasStatus = status != null && !status.isBlank();
        boolean includeInactiveForAll = hasStatus && status.equalsIgnoreCase("all");
        boolean availableFlag = !hasStatus && "true".equalsIgnoreCase(available);

        List<Map<String, Object>> items = new ArrayList<>();

        for (DiscountCode dc : discountCodeRepository.findAll()) {
            boolean nameMatch = searchByName == null || searchByName.isBlank()
                    || dc.getCode().toLowerCase().contains(searchByName.toLowerCase())
                    || (dc.getDescription() != null && dc.getDescription().toLowerCase().contains(searchByName.toLowerCase()));
            if (!nameMatch) continue;

            boolean isCurrent = Boolean.TRUE.equals(dc.getActive()) && dc.getStartsAt() != null && dc.getEndsAt() != null
                    && (dc.getStartsAt().isBefore(now) || dc.getStartsAt().isEqual(now))
                    && (dc.getEndsAt().isAfter(now) || dc.getEndsAt().isEqual(now));
            boolean isUpcoming = Boolean.TRUE.equals(dc.getActive()) && dc.getStartsAt() != null && dc.getStartsAt().isAfter(now);
            boolean isAvailable = Boolean.TRUE.equals(dc.getActive()) && dc.getEndsAt() != null && (dc.getEndsAt().isAfter(now) || dc.getEndsAt().isEqual(now));

            boolean matched = true;
            if (includeInactiveForAll) {
                matched = true;
            } else if (hasStatus) {
                if (status.equalsIgnoreCase("current")) matched = isCurrent;
                else if (status.equalsIgnoreCase("upcoming")) matched = isUpcoming;
                else if (status.equalsIgnoreCase("available") || status.equalsIgnoreCase("current_or_upcoming")) matched = isAvailable;
                else if (status.equalsIgnoreCase("all")) matched = true;
            } else if (availableFlag) {
                matched = isAvailable;
            }
            if (!matched) continue;

            items.add(Map.of(
                    "id", dc.getDiscountCodeId(),
                    "kind", "CODE",
                    "title", dc.getCode(),
                    "description", dc.getDescription(),
                    "discountType", dc.getDiscountType() != null ? dc.getDiscountType().name() : null,
                    "discountValue", dc.getDiscountValue(),
                    "startsAt", dc.getStartsAt(),
                    "endsAt", dc.getEndsAt(),
                    "active", dc.getActive(),
                    "createdAt", dc.getCreatedAt()
            ));
        }

        for (DiscountEvent ev : discountEventRepository.findAll()) {
            boolean nameMatch = searchByName == null || searchByName.isBlank()
                    || ev.getName().toLowerCase().contains(searchByName.toLowerCase())
                    || (ev.getDescription() != null && ev.getDescription().toLowerCase().contains(searchByName.toLowerCase()));
            if (!nameMatch) continue;

            boolean isCurrent = Boolean.TRUE.equals(ev.getActive()) && ev.getStartsAt() != null && ev.getEndsAt() != null
                    && (ev.getStartsAt().isBefore(now) || ev.getStartsAt().isEqual(now))
                    && (ev.getEndsAt().isAfter(now) || ev.getEndsAt().isEqual(now));
            boolean isUpcoming = Boolean.TRUE.equals(ev.getActive()) && ev.getStartsAt() != null && ev.getStartsAt().isAfter(now);
            boolean isAvailable = Boolean.TRUE.equals(ev.getActive()) && ev.getEndsAt() != null && (ev.getEndsAt().isAfter(now) || ev.getEndsAt().isEqual(now));

            boolean matchedEv = true;
            if (includeInactiveForAll) {
                matchedEv = true;
            } else if (hasStatus) {
                if (status.equalsIgnoreCase("current")) matchedEv = isCurrent;
                else if (status.equalsIgnoreCase("upcoming")) matchedEv = isUpcoming;
                else if (status.equalsIgnoreCase("available") || status.equalsIgnoreCase("current_or_upcoming")) matchedEv = isAvailable;
                else if (status.equalsIgnoreCase("all")) matchedEv = true;
            } else if (availableFlag) {
                matchedEv = isAvailable;
            }
            if (!matchedEv) continue;

            items.add(Map.of(
                    "id", ev.getDiscountEventId(),
                    "kind", "EVENT",
                    "title", ev.getName(),
                    "description", ev.getDescription(),
                    "discountType", ev.getDiscountType() != null ? ev.getDiscountType().name() : null,
                    "discountValue", ev.getDiscountValue(),
                    "startsAt", ev.getStartsAt(),
                    "endsAt", ev.getEndsAt(),
                    "active", ev.getActive(),
                    "createdAt", ev.getCreatedAt()
            ));
        }

        items.sort(Comparator.comparing((Map<String, Object> m) -> (LocalDateTime) (m.get("startsAt") != null ? m.get("startsAt") : m.get("createdAt")))
                .reversed());

        int safeLimit = Math.max(1, limit == null ? 8 : limit);
        int safePage = Math.max(1, page == null ? 1 : page);
        int from = Math.min((safePage - 1) * safeLimit, Math.max(items.size() - 1, 0));
        int to = Math.min(from + safeLimit, items.size());
        List<Map<String, Object>> pageItems = from >= to ? List.of() : items.subList(from, to);

        int totalPages = (int) Math.ceil(items.size() / (double) safeLimit);
        Map<String, Object> meta = Map.of(
                "currentPage", safePage,
                "totalPage", totalPages,
                "prev", safePage > 1,
                "next", safePage < totalPages
        );

        return Map.of(
                "data", pageItems,
                "meta", meta
        );
    }

    @Override
    @Transactional
    public DiscountCode create(CreateCodeDTO body) {
        if (body == null || body.getCoupon_code() == null || body.getCoupon_code().isBlank()) {
            throw new IllegalArgumentException("coupon_code is required");
        }
        if (discountCodeRepository.findByCodeIgnoreCase(body.getCoupon_code().trim()).isPresent()) {
            throw new IllegalStateException("coupon_code already exists");
        }
        DiscountCode dc = new DiscountCode();
        dc.setCode(body.getCoupon_code().trim().toUpperCase());
        dc.setDescription(body.getDesc());
        DiscountType type = parseDiscountType(body.getDiscount_type());
        BigDecimal value = parseDecimal(body.getDiscount_value(), null);
        if (value == null && body.getDiscount() != null && !body.getDiscount().isBlank()) {
            type = DiscountType.PERCENT;
            value = parseDecimal(body.getDiscount(), BigDecimal.ZERO);
        }
        dc.setDiscountType(type);
        dc.setDiscountValue(value != null ? value : BigDecimal.ZERO);
        dc.setMinOrderAmount(parseDecimal(body.getMin_order_amount(), null));
        dc.setStartsAt(parseDateTime(body.getStart_date()));
        dc.setEndsAt(parseDateTime(body.getEnd_date()));
        dc.setTotalUsageLimit(parseInteger(body.getTotal_usage_limit(), null));
        dc.setPerUserLimit(null);
        dc.setActive(true);
        dc.setUsageCount(0);
        dc.setCreatedAt(LocalDateTime.now());
        discountCodeRepository.save(dc);
        return dc;
    }

    @Override
    @Transactional
    public DiscountEvent createEvent(CreateEventDTO body) {
        if (body == null || body.getName() == null || body.getName().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (body.getProduct_ids() == null || body.getProduct_ids().isEmpty()) {
            throw new IllegalArgumentException("product_ids required");
        }
        DiscountEvent ev = new DiscountEvent();
        ev.setName(body.getName());
        ev.setDescription(body.getDesc());
        ev.setDiscountType(parseDiscountType(body.getDiscount_type()));
        ev.setDiscountValue(parseDecimal(body.getDiscount_value(), BigDecimal.ZERO));
        ev.setStartsAt(parseDateTime(body.getStart_date()));
        ev.setEndsAt(parseDateTime(body.getEnd_date()));
        ev.setActive(true);
        ev.setCreatedAt(LocalDateTime.now());
        for (Integer pid : body.getProduct_ids()) {
            Product p = productRepository.findById(pid).orElse(null);
            if (p == null) continue;
            DiscountEventProduct dep = new DiscountEventProduct();
            dep.setDiscountEvent(ev);
            dep.setProduct(p);
            ev.getProducts().add(dep);
        }
        discountEventRepository.save(ev);
        return ev;
    }

    @Override
    @Transactional(readOnly = true)
    public PromoDetailDTO getOne(Integer id, String kind) {
        // If client specifies kind, honor it to avoid cross-table id collisions
        if (kind != null) {
            if ("CODE".equalsIgnoreCase(kind)) {
                var dcExact = discountCodeRepository.findById(id).orElse(null);
                if (dcExact != null) {
                    PromoDetailDTO dto = new PromoDetailDTO();
                    dto.setId(dcExact.getDiscountCodeId());
                    dto.setKind("CODE");
                    dto.setTitle(dcExact.getCode());
                    dto.setCouponCode(dcExact.getCode());
                    dto.setDescription(dcExact.getDescription());
                    dto.setDiscountType(dcExact.getDiscountType() != null ? dcExact.getDiscountType().name() : null);
                    dto.setDiscountValue(dcExact.getDiscountValue());
                    dto.setMinOrderAmount(dcExact.getMinOrderAmount());
                    dto.setTotalUsageLimit(dcExact.getTotalUsageLimit());
                    dto.setStartsAt(dcExact.getStartsAt());
                    dto.setEndsAt(dcExact.getEndsAt());
                    dto.setActive(dcExact.getActive());
                    return dto;
                }
            } else if ("EVENT".equalsIgnoreCase(kind)) {
                var evExact = discountEventRepository.findById(id).orElse(null);
                if (evExact != null) {
                    List<Integer> pids = new ArrayList<>();
                    List<PromoProductDTO> plist = new ArrayList<>();
                    for (var dep : evExact.getProducts()) {
                        var p = dep.getProduct();
                        if (p == null) continue;
                        pids.add(p.getProductId());
                        plist.add(new PromoProductDTO(
                                p.getProductId(),
                                p.getName(),
                                p.getPrice(),
                                p.getCategory() != null ? p.getCategory().getName() : null
                        ));
                    }
                    PromoDetailDTO dto = new PromoDetailDTO();
                    dto.setId(evExact.getDiscountEventId());
                    dto.setKind("EVENT");
                    dto.setTitle(evExact.getName());
                    dto.setDescription(evExact.getDescription());
                    dto.setDiscountType(evExact.getDiscountType() != null ? evExact.getDiscountType().name() : null);
                    dto.setDiscountValue(evExact.getDiscountValue());
                    dto.setStartsAt(evExact.getStartsAt());
                    dto.setEndsAt(evExact.getEndsAt());
                    dto.setActive(evExact.getActive());
                    dto.setProductIds(pids);
                    dto.setProducts(plist);
                    return dto;
                }
            }
        }
        throw new NoSuchElementException("Promo not found");
    }

    @Override
    @Transactional
    public void update(Integer id, UpdatePromoDTO body) {
        var dc = discountCodeRepository.findById(id).orElse(null);
        if (dc != null) {
            if (body.getCoupon_code() != null && !body.getCoupon_code().isBlank()) dc.setCode(body.getCoupon_code().trim().toUpperCase());
            if (body.getDesc() != null) dc.setDescription(body.getDesc());
            if (body.getDiscount_type() != null) dc.setDiscountType(parseDiscountType(body.getDiscount_type()));
            if (body.getDiscount_value() != null) dc.setDiscountValue(parseDecimal(body.getDiscount_value(), dc.getDiscountValue()));
            if (body.getMin_order_amount() != null) dc.setMinOrderAmount(parseDecimal(body.getMin_order_amount(), dc.getMinOrderAmount()));
            if (body.getTotal_usage_limit() != null) dc.setTotalUsageLimit(parseInteger(body.getTotal_usage_limit(), dc.getTotalUsageLimit()));
            if (body.getStart_date() != null) dc.setStartsAt(parseDateTime(body.getStart_date()));
            if (body.getEnd_date() != null) dc.setEndsAt(parseDateTime(body.getEnd_date()));
            discountCodeRepository.save(dc);
            return;
        }
        var ev = discountEventRepository.findById(id).orElse(null);
        if (ev != null) {
            if (body.getName() != null) ev.setName(body.getName());
            if (body.getDesc() != null) ev.setDescription(body.getDesc());
            if (body.getDiscount_type() != null) ev.setDiscountType(parseDiscountType(body.getDiscount_type()));
            if (body.getDiscount_value() != null) ev.setDiscountValue(parseDecimal(body.getDiscount_value(), ev.getDiscountValue()));
            if (body.getStart_date() != null) ev.setStartsAt(parseDateTime(body.getStart_date()));
            if (body.getEnd_date() != null) ev.setEndsAt(parseDateTime(body.getEnd_date()));
            if (body.getProduct_ids() != null) {
                ev.getProducts().clear();
                for (Integer pid : body.getProduct_ids()) {
                    Product p = productRepository.findById(pid).orElse(null);
                    if (p == null) continue;
                    DiscountEventProduct dep = new DiscountEventProduct();
                    dep.setDiscountEvent(ev);
                    dep.setProduct(p);
                    ev.getProducts().add(dep);
                }
            }
            discountEventRepository.save(ev);
            return;
        }
        throw new NoSuchElementException("Promo not found");
    }

    @Override
    @Transactional
    public void softDelete(Integer id) {
        var dc = discountCodeRepository.findById(id).orElse(null);
        if (dc != null) {
            dc.setActive(false);
            discountCodeRepository.save(dc);
            return;
        }
        var ev = discountEventRepository.findById(id).orElse(null);
        if (ev != null) {
            ev.setActive(false);
            discountEventRepository.save(ev);
            return;
        }
        throw new NoSuchElementException("Promo not found");
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
        try {
            OffsetDateTime odt = OffsetDateTime.parse(raw);
            return odt.toLocalDateTime();
        } catch (Exception ignored) {}
        try {
            Instant inst = Instant.parse(raw);
            return LocalDateTime.ofInstant(inst, ZoneId.systemDefault());
        } catch (Exception ignored) {}
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ignored) {}
        try {
            LocalDate d = LocalDate.parse(raw, DateTimeFormatter.ISO_DATE);
            return d.atStartOfDay();
        } catch (Exception ignored) {}
        return null;
    }
}


