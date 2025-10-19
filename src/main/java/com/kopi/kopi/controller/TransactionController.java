package com.kopi.kopi.controller;

import com.kopi.kopi.entity.*;
import com.kopi.kopi.entity.enums.PaymentMethod;
import com.kopi.kopi.entity.enums.PaymentStatus;
import com.kopi.kopi.repository.AddressRepository;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.repository.ProductRepository;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/apiv1")
public class TransactionController {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    public TransactionController(OrderRepository orderRepository, ProductRepository productRepository, AddressRepository addressRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/userPanel/transactions")
    public Map<String, Object> getUserTransactions(
            @RequestParam(name = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(name = "limit", required = false, defaultValue = "9") Integer limit
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();

        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(limit, 1));
        Page<OrderEntity> pageData = orderRepository.findByCustomer_UserId(userId, pageable);

        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderEntity o : pageData.getContent()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getOrderId());
            m.put("grand_total", defaultBigDecimal(o.getTotalAmount()));
            m.put("status_name", o.getStatus());

            List<Map<String, Object>> products = new ArrayList<>();
            if (o.getOrderDetails() != null && !o.getOrderDetails().isEmpty()) {
                for (OrderDetail d : o.getOrderDetails()) {
                    Product p = d.getProduct();
                    Map<String, Object> pd = new HashMap<>();
                    pd.put("product_name", d.getProductNameSnapshot() != null ? d.getProductNameSnapshot() : (p != null ? p.getName() : null));
                    pd.put("product_img", p != null ? p.getImgUrl() : null);
                    pd.put("qty", d.getQuantity());
                    pd.put("subtotal", defaultBigDecimal(d.getLineTotal()));
                    pd.put("size", "Regular");
                    products.add(pd);
                }
            }
            m.put("products", products);
            items.add(m);
        }

        Map<String, Object> meta = new HashMap<>();
        meta.put("currentPage", pageData.getNumber() + 1);
        meta.put("totalPage", pageData.getTotalPages());
        meta.put("prev", pageData.hasPrevious());
        meta.put("next", pageData.hasNext());

        return Map.of("data", items, "meta", meta);
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<?> getTransactionDetail(@PathVariable("id") Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();

        OrderEntity o = orderRepository.findById(id).orElseThrow();
        if (o.getCustomer() == null || !Objects.equals(o.getCustomer().getUserId(), userId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }

        Map<String, Object> detail = new HashMap<>();
        detail.put("id", o.getOrderId());
        detail.put("receiver_email", "");
        detail.put("receiver_name", o.getCustomer() != null ? o.getCustomer().getFullName() : "");
        detail.put("delivery_address", o.getAddress() != null ? o.getAddress().getAddressLine() : "");
        detail.put("notes", o.getNote());
        detail.put("status_id", 0);
        detail.put("status_name", o.getStatus());
        detail.put("transaction_time", o.getCreatedAt());

        String paymentName = null;
        BigDecimal paymentFee = BigDecimal.ZERO;
        if (o.getPayments() != null && !o.getPayments().isEmpty()) {
            Payment p = o.getPayments().get(0);
            paymentName = p.getMethod() != null ? p.getMethod().name() : null;
        }
        detail.put("payment_id", 0);
        detail.put("payment_name", paymentName);
        detail.put("payment_fee", paymentFee);

        detail.put("delivery_name", "");
        detail.put("delivery_fee", BigDecimal.ZERO);
        detail.put("grand_total", defaultBigDecimal(o.getTotalAmount()));

        List<Map<String, Object>> products = new ArrayList<>();
        if (o.getOrderDetails() != null) {
            for (OrderDetail d : o.getOrderDetails()) {
                Product p = d.getProduct();
                Map<String, Object> pd = new HashMap<>();
                pd.put("id", d.getOrderDetailId());
                pd.put("product_name", d.getProductNameSnapshot() != null ? d.getProductNameSnapshot() : (p != null ? p.getName() : null));
                pd.put("product_img", p != null ? p.getImgUrl() : null);
                pd.put("qty", d.getQuantity());
                pd.put("size", "Regular");
                pd.put("subtotal", defaultBigDecimal(d.getLineTotal()));
                products.add(pd);
            }
        }
        detail.put("products", products);

        return ResponseEntity.ok(Map.of("data", List.of(detail)));
    }

    @GetMapping("/transactions")
    public Map<String, Object> listPending(
            @RequestParam(name = "status", required = false, defaultValue = "PENDING") String status,
            @RequestParam(name = "page", required = false, defaultValue = "1") Integer page,
            @RequestParam(name = "limit", required = false, defaultValue = "20") Integer limit
    ) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(limit, 1));
        Page<OrderEntity> pageData = orderRepository.findByStatus(status, pageable);

        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderEntity o : pageData.getContent()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getOrderId());
            m.put("status", o.getStatus());
            m.put("address", o.getAddress() != null ? o.getAddress().getAddressLine() : null);
            m.put("created_at", o.getCreatedAt());
            m.put("total", defaultBigDecimal(o.getTotalAmount()));

            List<Map<String, Object>> products = new ArrayList<>();
            if (o.getOrderDetails() != null) {
                for (OrderDetail d : o.getOrderDetails()) {
                    Map<String, Object> pd = new HashMap<>();
                    pd.put("product_name", d.getProductNameSnapshot());
                    pd.put("qty", d.getQuantity());
                    pd.put("subtotal", defaultBigDecimal(d.getLineTotal()));
                    products.add(pd);
                }
            }
            m.put("products", products);
            items.add(m);
        }

        Map<String, Object> meta = new HashMap<>();
        meta.put("currentPage", pageData.getNumber() + 1);
        meta.put("totalPage", pageData.getTotalPages());
        meta.put("prev", pageData.hasPrevious());
        meta.put("next", pageData.hasNext());
        return Map.of("data", items, "meta", meta);
    }

    @PatchMapping("/transactions/{id}/status")
    public ResponseEntity<?> changeStatus(
            @PathVariable("id") Integer id,
            @RequestBody Map<String, Object> payload
    ) {
        String status = String.valueOf(payload.getOrDefault("status", ""));
        if (!Objects.equals(status, "COMPLETED") && !Objects.equals(status, "CANCELLED") && !Objects.equals(status, "PENDING")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid status"));
        }
        OrderEntity order = orderRepository.findById(id).orElseThrow();
        order.setStatus(status);
        order.setUpdatedAt(java.time.LocalDateTime.now());
        // Update payment status in tandem
        if (order.getPayments() != null && !order.getPayments().isEmpty()) {
            Payment payment = order.getPayments().get(0);
            if (Objects.equals(status, "COMPLETED")) {
                payment.setStatus(PaymentStatus.PAID);
            } else if (Objects.equals(status, "CANCELLED")) {
                payment.setStatus(PaymentStatus.CANCELLED);
            } else if (Objects.equals(status, "PENDING")) {
                payment.setStatus(PaymentStatus.PENDING);
            }
        }
        orderRepository.save(order);
        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    @PostMapping("/transactions")
    public ResponseEntity<?> createTransaction(@RequestBody Map<String, Object> body) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User current = ((UserPrincipal) auth.getPrincipal()).getUser();
        Integer userId = current.getUserId();
        Integer roleId = current.getRole() != null ? current.getRole().getRoleId() : null;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) body.getOrDefault("products", List.of());
        String notes = (String) body.getOrDefault("notes", "");
        String addressText = (String) body.getOrDefault("address", "");
        Integer paymentId = Integer.valueOf(String.valueOf(body.getOrDefault("payment_id", 1)));
        boolean paid = false;
        try { paid = Boolean.parseBoolean(String.valueOf(body.getOrDefault("paid", false))); } catch (Exception ignored) {}
        Integer customerIdFromBody = null;
        if (body.containsKey("customer_id") && body.get("customer_id") != null) {
            try { customerIdFromBody = Integer.valueOf(String.valueOf(body.get("customer_id"))); } catch (Exception ignored) {}
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderDetail> details = new ArrayList<>();
        for (Map<String, Object> p : products) {
            Integer productId = Integer.valueOf(String.valueOf(p.get("product_id")));
            Integer qty = Integer.valueOf(String.valueOf(p.getOrDefault("qty", 1)));
            Product prod = productRepository.findById(productId).orElseThrow();
            BigDecimal unit = prod.getPrice();
            subtotal = subtotal.add(unit.multiply(BigDecimal.valueOf(qty)));
            OrderDetail d = OrderDetail.builder()
                    .product(prod)
                    .productNameSnapshot(prod.getName())
                    .unitPrice(unit)
                    .quantity(qty)
                    .build();
            details.add(d);
        }

        Address addr = null;
        User customer = null;
        // Determine flow: customer (role_id = 3) vs staff (role_id = 2)
        if (roleId != null && roleId == 3) {
            // Customer placing order: customer = current user, created_by = current user, address optional
            customer = userRepository.findById(userId).orElse(null);
            if (addressText != null && !addressText.isBlank()) {
                addr = Address.builder()
                        .addressLine(addressText)
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
                addr = addressRepository.save(addr);
            }
        } else if (roleId != null && roleId == 2) {
            // Staff placing order: if customer_id is provided, attach customer; if null, keep null; address must be null when customer is null
            if (customerIdFromBody != null) {
                customer = userRepository.findById(customerIdFromBody).orElse(null);
                if (addressText != null && !addressText.isBlank()) {
                    addr = Address.builder()
                            .addressLine(addressText)
                            .createdAt(java.time.LocalDateTime.now())
                            .build();
                    addr = addressRepository.save(addr);
                }
            }
        } else {
            // Fallback: treat like customer, but keep safe
            customer = userRepository.findById(userId).orElse(null);
            if (addressText != null && !addressText.isBlank()) {
                addr = Address.builder()
                        .addressLine(addressText)
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
                addr = addressRepository.save(addr);
            }
        }

        OrderEntity order = OrderEntity.builder()
                .orderCode("ORD-" + System.currentTimeMillis())
                .status(paid ? "COMPLETED" : "PENDING")
                .subtotalAmount(subtotal)
                .discountAmount(BigDecimal.ZERO)
                .note(notes)
                .createdAt(java.time.LocalDateTime.now())
                .updatedAt(java.time.LocalDateTime.now())
                .address(addr)
                .customer(customer)
                .createdBy(userRepository.findById(userId).orElse(null))
                .build();

        for (OrderDetail d : details) {
            d.setOrder(order);
        }
        order.setOrderDetails(details);

        if (paymentId != null) {
            PaymentMethod method = PaymentMethod.CASH;
            if (paymentId == 2) method = PaymentMethod.BANKING;
            Payment payment = Payment.builder()
                    .order(order)
                    .amount(subtotal)
                    .method(method)
                    .status(paid ? PaymentStatus.PAID : PaymentStatus.PENDING)
                    .createdAt(java.time.LocalDateTime.now())
                    .build();
            order.getPayments().add(payment);
        }

        OrderEntity saved = orderRepository.save(order);
        return ResponseEntity.ok(Map.of("message", "OK", "data", Map.of("id", saved.getOrderId())));
    }

    private BigDecimal defaultBigDecimal(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}


