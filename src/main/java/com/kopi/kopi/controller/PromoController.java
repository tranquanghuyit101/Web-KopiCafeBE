package com.kopi.kopi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

import com.kopi.kopi.service.IPromoService;
import com.kopi.kopi.dto.promo.CreateCodeDTO;
import com.kopi.kopi.dto.promo.CreateEventDTO;
import com.kopi.kopi.dto.promo.UpdatePromoDTO;
import com.kopi.kopi.dto.promo.PromoDetailDTO;
import com.kopi.kopi.exception.ValidationException;

@RestController
@RequestMapping("/apiv1/promo")
public class PromoController {
    private final IPromoService promoService;

    public PromoController(IPromoService promoService) {
        this.promoService = promoService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody CreateCodeDTO body) {
        try {
            promoService.create(body);
            return ResponseEntity.ok(Map.of("message", "created"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/events")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createEvent(@RequestBody CreateEventDTO body) {
        try {
            promoService.createEvent(body);
            return ResponseEntity.ok(Map.of("message", "created"));
        } catch (ValidationException ve) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", ve.getMessage(),
                "invalid_products", ve.getInvalidProducts()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
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
    public ResponseEntity<?> getOne(@PathVariable("id") Integer id, @RequestParam("discount_kind") String discountKind) {
        try {
            PromoDetailDTO dto;
            if (discountKind != null && discountKind.equalsIgnoreCase("code")) {
                dto = promoService.getCodeDetail(id);
            } else if (discountKind != null && discountKind.equalsIgnoreCase("event")) {
                dto = promoService.getEventDetail(id);
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "discount_kind must be 'code' or 'event'"));
            }
            return ResponseEntity.ok(dto);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(
            @PathVariable("id") Integer id,
            @RequestParam("discount_kind") String discountKind,
            @RequestBody UpdatePromoDTO body
    ) {
        try {
            if (discountKind != null && discountKind.equalsIgnoreCase("code")) {
                promoService.updateCode(id, body);
            } else if (discountKind != null && discountKind.equalsIgnoreCase("event")) {
                promoService.updateEvent(id, body);
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "discount_kind must be 'code' or 'event'"));
            }
            return ResponseEntity.ok(Map.of("message", "updated"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            // e.g. attempting to edit a discount that is not upcoming
            return ResponseEntity.status(409).body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> softDelete(@PathVariable("id") Integer id) {
        try {
            promoService.softDelete(id);
            return ResponseEntity.ok(Map.of("message", "deleted"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Parsing helpers moved to service
}