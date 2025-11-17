package com.kopi.kopi.controller;

import com.kopi.kopi.entity.User;
import com.kopi.kopi.security.UserPrincipal;
import com.kopi.kopi.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.math.BigDecimal;

@RestController
@RequestMapping("/apiv1")
public class TransactionController {
    private final OrderService orderService;

    public TransactionController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/userPanel/transactions")
    public Map<String, Object> getUserTransactions(
            @RequestParam(name = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(name = "limit", required = false, defaultValue = "9") Integer limit
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();
        return orderService.getUserTransactions(userId, page, limit);
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<?> getTransactionDetail(@PathVariable("id") Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User current = ((UserPrincipal) auth.getPrincipal()).getUser();
        return orderService.getTransactionDetail(id, current);
    }

    @GetMapping("/transactions")
    public Map<String, Object> listPending(
            @RequestParam(name = "status", required = false, defaultValue = "PENDING") String status,
            @RequestParam(name = "type", required = false, defaultValue = "ALL") String type,
            @RequestParam(name = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(name = "limit", required = false, defaultValue = "20") Integer limit
    ) {
        return orderService.listPending(status, type, page, limit);
    }

    @PatchMapping("/transactions/{id}/status")
    public ResponseEntity<?> changeStatus(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> payload
    ) {
        return orderService.changeStatus(id, payload);
    }

    @PostMapping("/transactions")
    public ResponseEntity<?> createTransaction(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User current = ((UserPrincipal) auth.getPrincipal()).getUser();
        return orderService.createTransaction(body, current);
    }

    @PostMapping("/orders/validate")
    public ResponseEntity<?> validateProducts(@RequestBody Map<String, Object> body) {
        return orderService.validateProducts(body);
    }

    @PostMapping("/orders/validate-discount")
    public ResponseEntity<?> validateDiscount(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User current = ((UserPrincipal) auth.getPrincipal()).getUser();
        return orderService.validateDiscount(body, current);
    }

    private BigDecimal defaultBigDecimal(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}


