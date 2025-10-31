package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.DiningTable;
import com.kopi.kopi.repository.DiningTableRepository;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.service.TableService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

@Service
public class TableServiceImpl implements TableService {
    private final DiningTableRepository diningTableRepository;
    private final OrderRepository orderRepository;

    public TableServiceImpl(DiningTableRepository diningTableRepository, OrderRepository orderRepository) {
        this.diningTableRepository = diningTableRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public void setOccupiedIfHasPendingOrders(Integer tableId) {
        long pending = orderRepository.countByTable_TableIdAndStatus(tableId, "PENDING");
        diningTableRepository.findById(tableId).ifPresent(t -> {
            String newStatus = pending > 0 ? "OCCUPIED" : "AVAILABLE";
            if (!newStatus.equals(t.getStatus())) {
                t.setStatus(newStatus);
                t.setUpdatedAt(LocalDateTime.now());
                diningTableRepository.save(t);
            }
        });
    }

    @Override
    @Transactional
    public void setAvailableIfNoPendingOrders(Integer tableId) {
        long pending = orderRepository.countByTable_TableIdAndStatus(tableId, "PENDING");
        if (pending == 0) {
            diningTableRepository.findById(tableId).ifPresent(t -> {
                if (!"AVAILABLE".equals(t.getStatus())) {
                    t.setStatus("AVAILABLE");
                    t.setUpdatedAt(LocalDateTime.now());
                    diningTableRepository.save(t);
                }
            });
        }
    }

    @Override
    public Map<String, Object> list(Integer page, Integer limit, String status) {
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

    @Override
    @Transactional
    public ResponseEntity<?> create(Map<String, Object> body) {
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

    @Override
    @Transactional
    public ResponseEntity<?> patch(Integer id, Map<String, Object> body) {
        DiningTable t = diningTableRepository.findById(id).orElseThrow();
        if (body.containsKey("number") && body.get("number") != null) t.setNumber(Integer.valueOf(String.valueOf(body.get("number"))));
        if (body.containsKey("name")) t.setName(body.get("name") != null ? String.valueOf(body.get("name")) : null);
        if (body.containsKey("status") && body.get("status") != null) t.setStatus(String.valueOf(body.get("status")));
        t.setUpdatedAt(LocalDateTime.now());
        diningTableRepository.save(t);
        return ResponseEntity.ok(Map.of("data", t));
    }

    @Override
    @Transactional
    public ResponseEntity<?> delete(Integer id) {
        diningTableRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> rotateQr(Integer id) {
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

    @Override
    public ResponseEntity<?> getByQr(String qrToken) {
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


