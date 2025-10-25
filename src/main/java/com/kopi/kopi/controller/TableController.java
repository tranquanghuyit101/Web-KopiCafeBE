package com.kopi.kopi.controller;

import com.kopi.kopi.entity.DiningTable;
import com.kopi.kopi.repository.DiningTableRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

@RestController
@RequestMapping("/apiv1/tables")
public class TableController {
    private final DiningTableRepository diningTableRepository;

    public TableController(DiningTableRepository diningTableRepository) {
        this.diningTableRepository = diningTableRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public Map<String, Object> list(@RequestParam(name = "page", defaultValue = "1") Integer page,
                                    @RequestParam(name = "limit", defaultValue = "20") Integer limit,
                                    @RequestParam(name = "status", required = false) String status) {
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, limit));
        Page<DiningTable> pg = status == null || status.isBlank()
                ? diningTableRepository.findAll(pageable)
                : diningTableRepository.findByStatus(status, pageable);
        return Map.of(
                "data", pg.getContent(),
                "meta", Map.of(
                        "currentPage", pg.getNumber() + 1,
                        "totalPage", pg.getTotalPages(),
                        "prev", pg.hasPrevious(),
                        "next", pg.hasNext()
                )
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        Integer number = Integer.valueOf(String.valueOf(body.get("number")));
        String name = body.get("name") != null ? String.valueOf(body.get("name")) : null;
        String status = body.get("status") != null ? String.valueOf(body.get("status")) : "AVAILABLE";
        String qrToken = randomToken();
        DiningTable t = DiningTable.builder()
                .number(number)
                .name(name)
                .status(status)
                .qrToken(qrToken)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        t = diningTableRepository.save(t);
        return ResponseEntity.ok(Map.of("data", t));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> patch(@PathVariable("id") Integer id, @RequestBody Map<String, Object> body) {
        DiningTable t = diningTableRepository.findById(id).orElseThrow();
        if (body.containsKey("number") && body.get("number") != null) t.setNumber(Integer.valueOf(String.valueOf(body.get("number"))));
        if (body.containsKey("name")) t.setName(body.get("name") != null ? String.valueOf(body.get("name")) : null);
        if (body.containsKey("status") && body.get("status") != null) t.setStatus(String.valueOf(body.get("status")));
        t.setUpdatedAt(LocalDateTime.now());
        diningTableRepository.save(t);
        return ResponseEntity.ok(Map.of("data", t));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> delete(@PathVariable("id") Integer id) {
        diningTableRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    @PostMapping("/{id}/rotate-qr")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> rotateQr(@PathVariable("id") Integer id) {
        DiningTable t = diningTableRepository.findById(id).orElseThrow();
        t.setQrToken(randomToken());
        t.setUpdatedAt(LocalDateTime.now());
        diningTableRepository.save(t);
        return ResponseEntity.ok(Map.of(
                "data", Map.of(
                        "id", t.getTableId(),
                        "number", t.getNumber(),
                        "qr_token", t.getQrToken()
                )
        ));
    }

    @GetMapping("/by-qr/{qrToken}")
    public ResponseEntity<?> getByQr(@PathVariable("qrToken") String qrToken) {
        var t = diningTableRepository.findByQrToken(qrToken).orElse(null);
        if (t == null) return ResponseEntity.status(404).body(Map.of("message", "Not found"));
        return ResponseEntity.ok(Map.of("data", Map.of("id", t.getTableId(), "number", t.getNumber(), "status", t.getStatus())));
    }

    private static String randomToken() {
        byte[] b = new byte[16];
        new SecureRandom().nextBytes(b);
        return HexFormat.of().formatHex(b);
    }
}


