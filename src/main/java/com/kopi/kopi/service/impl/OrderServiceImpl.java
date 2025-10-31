package com.kopi.kopi.service.impl;

import com.kopi.kopi.controller.GuestOrderController;
import com.kopi.kopi.entity.*;
import com.kopi.kopi.entity.enums.PaymentMethod;
import com.kopi.kopi.entity.enums.PaymentStatus;
import com.kopi.kopi.repository.*;
import com.kopi.kopi.service.OrderService;
import com.kopi.kopi.service.TableService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final UserAddressRepository userAddressRepository;
    private final UserRepository userRepository;
    private final TableService tableService;
    private final DiningTableRepository diningTableRepository;

    public OrderServiceImpl(OrderRepository orderRepository, ProductRepository productRepository, AddressRepository addressRepository, UserRepository userRepository, TableService tableService, DiningTableRepository diningTableRepository, UserAddressRepository userAddressRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.tableService = tableService;
        this.diningTableRepository = diningTableRepository;
        this.userAddressRepository = userAddressRepository;
    }

    @Override
    public Map<String, Object> getUserTransactions(Integer userId, Integer page, Integer limit) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(limit, 1), Sort.by("createdAt").descending());
        Page<OrderEntity> pageData = orderRepository.findByCustomer_UserId(userId, pageable);

        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderEntity o : pageData.getContent()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getOrderId());
            m.put("grand_total", defaultBigDecimal(o.getTotalAmount()));
            m.put("status_name", o.getStatus());
            m.put("created_at", o.getCreatedAt());

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

    @Override
    public ResponseEntity<?> getTransactionDetail(Integer id, Integer userId) {
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

    @Override
    public Map<String, Object> listPending(String status, String type, Integer page, Integer limit) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(limit, 1));
        Page<OrderEntity> pageData;
        if ("TABLE".equalsIgnoreCase(type)) {
            pageData = orderRepository.findByStatusAndTableIsNotNull(status, pageable);
        } else if ("SHIPPING".equalsIgnoreCase(type)) {
            // Show orders with address and status NOT IN (CANCELLED, REJECTED, COMPLETED)
            pageData = orderRepository.findByStatusNotInAndAddressIsNotNull(List.of("CANCELLED", "REJECTED", "COMPLETED"), pageable);
        } else {
            pageData = orderRepository.findByStatus(status, pageable);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderEntity o : pageData.getContent()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getOrderId());
            m.put("status", o.getStatus());
            m.put("address", o.getAddress() != null ? o.getAddress().getAddressLine() : null);
            m.put("created_at", o.getCreatedAt());
            m.put("table_number", o.getTable() != null ? o.getTable().getNumber() : null);
            m.put("total", defaultBigDecimal(o.getTotalAmount()));
            m.put("shipper_id", o.getShipper() != null ? o.getShipper().getUserId() : null);

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

    @Override
    @Transactional
    public ResponseEntity<?> changeStatus(Integer id, Map<String, Object> payload) {
        String status = String.valueOf(payload.getOrDefault("status", ""));
        if (!Objects.equals(status, "COMPLETED") &&
                !Objects.equals(status, "CANCELLED") &&
                !Objects.equals(status, "PENDING") &&
                !Objects.equals(status, "ACCEPTED") &&
                !Objects.equals(status, "REJECTED") &&
                !Objects.equals(status, "READY") &&
                !Objects.equals(status, "SHIPPING") &&
                !Objects.equals(status, "PAID")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid status"));
        }
        OrderEntity order = orderRepository.findById(id).orElseThrow();
        String previousStatus = order.getStatus();
        // If moving to COMPLETED from a non-COMPLETED status: validate stock and deduct now
        if (Objects.equals(status, "COMPLETED") && !Objects.equals(previousStatus, "COMPLETED")) {
            if (order.getOrderDetails() != null) {
                // Validate
                for (OrderDetail d : order.getOrderDetails()) {
                    Product prod = d.getProduct();
                    Integer qty = d.getQuantity();
                    if (prod != null && prod.getStockQty() != null) {
                        if (prod.getStockQty() < qty) {
                            return ResponseEntity.badRequest().body(Map.of(
                                    "message", "Sản phẩm " + (prod.getName() == null ? "" : prod.getName()) + " không đủ số lượng trong kho!"
                            ));
                        }
                    }
                }
                // Deduct
                for (OrderDetail d : order.getOrderDetails()) {
                    Product prod = d.getProduct();
                    Integer qty = d.getQuantity();
                    if (prod != null && prod.getStockQty() != null) {
                        prod.setStockQty(prod.getStockQty() - qty);
                        productRepository.save(prod);
                    }
                }
            }
        }
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
            } else if (Objects.equals(status, "PAID")) {
                payment.setStatus(PaymentStatus.PAID);
            }
        }
        orderRepository.save(order);
        if (order.getTable() != null) {
            tableService.setAvailableIfNoPendingOrders(order.getTable().getTableId());
        }
        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> createTransaction(Map<String, Object> body, User current) {
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
        // 1. Kiểm tra tồn kho cho từng sản phẩm
        for (Map<String, Object> p : products) {
            Integer productId = Integer.valueOf(String.valueOf(p.get("product_id")));
            Integer qty = Integer.valueOf(String.valueOf(p.getOrDefault("qty", 1)));
            Product prod = productRepository.findById(productId).orElseThrow();
            if (prod.getStockQty() < qty) {
                return ResponseEntity.badRequest().body(Map.of("message",
                    "Sản phẩm " + prod.getName() + " không đủ số lượng trong kho!"));
            }
        }
        // 2. Tạo chi tiết order (KHÔNG trừ tồn kho ở bước confirm)
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
                // Reuse user's default address if present (to avoid duplicate address rows), else create a new one
                List<UserAddress> saved = userAddressRepository.findAllWithAddressByUserId(userId);
                if (saved != null && !saved.isEmpty() && saved.get(0).getAddress() != null) {
                    addr = saved.get(0).getAddress();
                } else {
                    addr = Address.builder()
                            .addressLine(addressText)
                            .createdAt(LocalDateTime.now())
                            .build();
                    addr = addressRepository.save(addr);
                }
            }
        } else if (roleId != null && roleId == 2) {
            // Staff placing order: if customer_id is provided, attach customer; if null, keep null; address must be null when customer is null
            if (customerIdFromBody != null) {
                customer = userRepository.findById(customerIdFromBody).orElse(null);
                if (addressText != null && !addressText.isBlank()) {
                    // Reuse customer's default address if present, else create
                    List<UserAddress> saved = userAddressRepository.findAllWithAddressByUserId(customerIdFromBody);
                    if (saved != null && !saved.isEmpty() && saved.get(0).getAddress() != null) {
                        addr = saved.get(0).getAddress();
                    } else {
                        addr = Address.builder()
                                .addressLine(addressText)
                                .createdAt(LocalDateTime.now())
                                .build();
                        addr = addressRepository.save(addr);
                    }
                }
            }
        } else {
            // Fallback: treat like customer, but keep safe
            customer = userRepository.findById(userId).orElse(null);
            if (addressText != null && !addressText.isBlank()) {
                List<UserAddress> saved = userAddressRepository.findAllWithAddressByUserId(userId);
                if (saved != null && !saved.isEmpty() && saved.get(0).getAddress() != null) {
                    addr = saved.get(0).getAddress();
                } else {
                    addr = Address.builder()
                            .addressLine(addressText)
                            .createdAt(LocalDateTime.now())
                            .build();
                    addr = addressRepository.save(addr);
                }
            }
        }

        OrderEntity order = OrderEntity.builder()
                .orderCode("ORD-" + System.currentTimeMillis())
                .status(paid ? "COMPLETED" : "PENDING")
                .subtotalAmount(subtotal)
                .discountAmount(BigDecimal.ZERO)
                .note(notes)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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
                    .createdAt(LocalDateTime.now())
                    .build();
            order.getPayments().add(payment);
        }

        OrderEntity saved = orderRepository.save(order);
        return ResponseEntity.ok(Map.of("message", "OK", "data", Map.of("id", saved.getOrderId())));
    }

    @Override
    @Transactional
    public ResponseEntity<?> createGuestTableOrder(GuestOrderController.GuestOrderRequest req) {
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

        List<GuestOrderController.GuestOrderItem> items = Optional.ofNullable(req.products()).orElse(List.of());
        if (items.isEmpty()) return ResponseEntity.badRequest().body(Map.of("message", "No products"));

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderDetail> details = new ArrayList<>();
        // 1. Kiểm tra tồn kho cho từng sản phẩm guest
        for (GuestOrderController.GuestOrderItem gi : items) {
            Integer productId = gi.product_id();
            Integer qty = gi.qty() == null ? 1 : gi.qty();
            Product prod = productRepository.findById(productId).orElseThrow();
            if (prod.getStockQty() < qty) {
                return ResponseEntity.badRequest().body(Map.of("message",
                    "Sản phẩm " + prod.getName() + " không đủ số lượng trong kho!"));
            }
        }
        // 2. Tạo chi tiết order (KHÔNG trừ tồn kho ở bước confirm)
        for (GuestOrderController.GuestOrderItem gi : items) {
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

    private BigDecimal defaultBigDecimal(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
    @Override
    public ResponseEntity<?> validateProducts(Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> products = (List<Map<String, Object>>) body.getOrDefault("products", List.of());
        if (products.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "No products"));
        }

        List<Map<String, Object>> insufficient = new ArrayList<>();
        for (Map<String, Object> p : products) {
            Integer productId = Integer.valueOf(String.valueOf(p.get("product_id")));
            Integer qty = Integer.valueOf(String.valueOf(p.getOrDefault("qty", 1)));
            Product prod = productRepository.findById(productId).orElse(null);
            if (prod == null) {
                insufficient.add(Map.of("product_id", productId, "reason", "not_found"));
                continue;
            }
            if (prod.getStockQty() == null || prod.getStockQty() < qty) {
                insufficient.add(Map.of(
                        "product_id", productId,
                        "name", prod.getName(),
                        "requested", qty,
                        "available", prod.getStockQty() == null ? 0 : prod.getStockQty()
                ));
            }
        }

        if (!insufficient.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Insufficient stock",
                    "errors", insufficient
            ));
        }
        return ResponseEntity.ok(Map.of("message", "OK"));
    }
}
