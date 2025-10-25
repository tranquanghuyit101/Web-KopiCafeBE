package com.kopi.kopi.controller;

import com.kopi.kopi.entity.*;
import com.kopi.kopi.entity.enums.PaymentMethod;
import com.kopi.kopi.entity.enums.PaymentStatus;
import com.kopi.kopi.repository.DiningTableRepository;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.repository.ProductRepository;
import com.kopi.kopi.service.TableService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/apiv1/guest")
public class GuestOrderController {
    private final DiningTableRepository diningTableRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final TableService tableService;

    public GuestOrderController(DiningTableRepository diningTableRepository, ProductRepository productRepository, OrderRepository orderRepository, TableService tableService) {
        this.diningTableRepository = diningTableRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.tableService = tableService;
    }

    public record GuestOrderItem(Integer product_id, Integer qty) {}
    public record GuestOrderRequest(String qr_token, Integer table_number, List<GuestOrderItem> products, String notes, Integer payment_id, Boolean paid) {}

    @PostMapping("/table-orders")
    @Transactional
    public ResponseEntity<?> createGuestTableOrder(@RequestBody GuestOrderRequest req) {
        if ((req.qr_token() == null || req.qr_token().isBlank()) && req.table_number() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "qr_token or table_number is required"));
        }

        DiningTable table = null;
        if (req.qr_token() != null && !req.qr_token().isBlank()) {
            table = diningTableRepository.findByQrToken(req.qr_token()).orElse(null);
        }
        if (table == null && req.table_number() != null) {
            table = diningTableRepository.findByNumber(req.table_number()).orElse(null);
        }
        if (table == null) return ResponseEntity.status(404).body(Map.of("message", "Table not found"));
        if (Objects.equals(table.getStatus(), "DISABLED")) return ResponseEntity.badRequest().body(Map.of("message", "Table disabled"));

        List<GuestOrderItem> items = Optional.ofNullable(req.products()).orElse(List.of());
        if (items.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "No products"));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderDetail> details = new ArrayList<>();
        for (GuestOrderItem gi : items) {
            Integer productId = gi.product_id();
            Integer qty = gi.qty() == null ? 1 : gi.qty();
            Product prod = productRepository.findById(productId).orElseThrow();
            BigDecimal unit = prod.getPrice();
            subtotal = subtotal.add(unit.multiply(BigDecimal.valueOf(qty)));
            details.add(OrderDetail.builder()
                    .product(prod)
                    .productNameSnapshot(prod.getName())
                    .unitPrice(unit)
                    .quantity(qty)
                    .build());
        }

        OrderEntity order = OrderEntity.builder()
                .orderCode("ORD-" + System.currentTimeMillis())
                .status(Boolean.TRUE.equals(req.paid()) ? "COMPLETED" : "PENDING")
                .subtotalAmount(subtotal)
                .discountAmount(BigDecimal.ZERO)
                .note(req.notes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .table(table)
                .build();

        for (OrderDetail d : details) d.setOrder(order);
        order.setOrderDetails(details);

        PaymentMethod method = PaymentMethod.CASH;
        if (req.payment_id() != null && req.payment_id() == 2) method = PaymentMethod.BANKING;
        Payment payment = Payment.builder()
                .order(order)
                .amount(subtotal)
                .method(method)
                .status(Boolean.TRUE.equals(req.paid()) ? PaymentStatus.PAID : PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        order.getPayments().add(payment);

        OrderEntity saved = orderRepository.save(order);
        tableService.setOccupiedIfHasPendingOrders(table.getTableId());
        return ResponseEntity.ok(Map.of("message", "OK", "data", Map.of(
                "id", saved.getOrderId(),
                "table_number", table.getNumber(),
                "status", saved.getStatus()
        )));
    }
}


