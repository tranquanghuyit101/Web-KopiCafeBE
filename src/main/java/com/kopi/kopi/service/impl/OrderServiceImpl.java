package com.kopi.kopi.service.impl;

import com.kopi.kopi.controller.GuestOrderController;
import com.kopi.kopi.entity.*;
import com.kopi.kopi.entity.enums.PaymentMethod;
import com.kopi.kopi.entity.enums.PaymentStatus;
import com.kopi.kopi.repository.*;
import com.kopi.kopi.service.NotificationService;
import com.kopi.kopi.service.OrderService;
import com.kopi.kopi.service.TableService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.EntityManager;
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
    private final ProductSizeRepository productSizeRepository;
    private final ProductAddOnRepository productAddOnRepository;
    private final SizeRepository sizeRepository;
    private final OrderDetailAddOnRepository orderDetailAddOnRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final DiscountCodeRedemptionRepository discountCodeRedemptionRepository;
    private final DiscountEventRepository discountEventRepository;
    @PersistenceContext
    private EntityManager entityManager;
    private final MapboxService mapboxService;
    private final NotificationService notificationService;


    public OrderServiceImpl(OrderRepository orderRepository, ProductRepository productRepository, AddressRepository addressRepository, UserRepository userRepository, TableService tableService, DiningTableRepository diningTableRepository, UserAddressRepository userAddressRepository, MapboxService mapboxService, NotificationService notificationService, ProductSizeRepository productSizeRepository, ProductAddOnRepository productAddOnRepository, SizeRepository sizeRepository, OrderDetailAddOnRepository orderDetailAddOnRepository, DiscountCodeRepository discountCodeRepository, DiscountCodeRedemptionRepository discountCodeRedemptionRepository, DiscountEventRepository discountEventRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.tableService = tableService;
        this.diningTableRepository = diningTableRepository;
        this.userAddressRepository = userAddressRepository;
        this.mapboxService = mapboxService;
        this.productSizeRepository = productSizeRepository;
        this.productAddOnRepository = productAddOnRepository;
        this.sizeRepository = sizeRepository;
        this.orderDetailAddOnRepository = orderDetailAddOnRepository;
        this.discountCodeRepository = discountCodeRepository;
        this.discountCodeRedemptionRepository = discountCodeRedemptionRepository;
        this.notificationService = notificationService;
        this.discountEventRepository = discountEventRepository;
    }

    private BigDecimal getDiscountedBasePrice(Product prod) {
        if (prod == null) return BigDecimal.ZERO;
        BigDecimal base = prod.getPrice() != null ? prod.getPrice() : BigDecimal.ZERO;
        try {
            var evOpt = discountEventRepository.findActiveEventByProductId(prod.getProductId(), java.time.LocalDateTime.now());
            if (evOpt.isEmpty()) return base;
            var ev = evOpt.get();
            if (ev.getDiscountType() == null || ev.getDiscountValue() == null) return base;
            switch (ev.getDiscountType()) {
                case PERCENT -> {
                    BigDecimal pct = ev.getDiscountValue();
                    if (pct.compareTo(BigDecimal.ZERO) < 0) pct = BigDecimal.ZERO;
                    if (pct.compareTo(new BigDecimal("100")) > 0) pct = new BigDecimal("100");
                    BigDecimal multiplier = BigDecimal.ONE.subtract(pct.divide(new BigDecimal("100")));
                    BigDecimal result = base.multiply(multiplier);
                    return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
                }
                case AMOUNT -> {
                    BigDecimal result = base.subtract(ev.getDiscountValue());
                    return result.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : result;
                }
                default -> { return base; }
            }
        } catch (Exception ignored) {
            return base;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getUserTransactions(Integer userId, Integer page, Integer limit) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(limit, 1), Sort.by("createdAt").descending());
        Page<OrderEntity> pageData = orderRepository.findByCustomer_UserId(userId, pageable);

        List<Map<String, Object>> items = new ArrayList<>();
        for (OrderEntity o : pageData.getContent()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getOrderId());
            m.put("grand_total", defaultBigDecimal(o.getTotalAmount()));
            m.put("subtotal", defaultBigDecimal(o.getSubtotalAmount()));
            m.put("shipping_fee", defaultBigDecimal(o.getShippingAmount()));
            m.put("discount", defaultBigDecimal(o.getDiscountAmount()));
            m.put("status_name", o.getStatus());
            m.put("created_at", o.getCreatedAt());
            // Payment
            String paymentName = null;
            if (o.getPayments() != null && !o.getPayments().isEmpty()) {
                Payment p = o.getPayments().get(0);
                paymentName = p.getMethod() != null ? p.getMethod().name() : null;
            }
            m.put("payment_name", paymentName);
            // Delivery
            String deliveryName = o.getAddress() != null ? "Shipping" : (o.getTable() != null ? ("Table " + o.getTable().getNumber()) : "");
            m.put("delivery_name", deliveryName);
            m.put("delivery_address", o.getAddress() != null ? o.getAddress().getAddressLine() : null);

            List<Map<String, Object>> products = new ArrayList<>();
            if (o.getOrderDetails() != null && !o.getOrderDetails().isEmpty()) {
                for (OrderDetail d : o.getOrderDetails()) {
                    Product p = d.getProduct();
                    Map<String, Object> pd = new HashMap<>();
                    pd.put("product_name", d.getProductNameSnapshot() != null ? d.getProductNameSnapshot() : (p != null ? p.getName() : null));
                    pd.put("product_img", p != null ? p.getImgUrl() : null);
                    pd.put("qty", d.getQuantity());
                    pd.put("subtotal", defaultBigDecimal(d.getLineTotal()));
                    pd.put("size", d.getSize() != null ? d.getSize().getName() : null);
                    // add-ons
                    List<Map<String, Object>> addOns = new ArrayList<>();
                    for (OrderDetailAddOn oda : orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(d.getOrderDetailId())) {
                        Map<String, Object> ao = new HashMap<>();
                        ao.put("name", oda.getAddOn() != null ? oda.getAddOn().getName() : null);
                        ao.put("price", defaultBigDecimal(oda.getUnitPriceSnapshot()));
                        addOns.add(ao);
                    }
                    pd.put("add_ons", addOns);
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
    @Transactional(readOnly = true)
    public ResponseEntity<?> getTransactionDetail(Integer id, User current) {
        OrderEntity o = orderRepository.findById(id).orElseThrow();
        boolean isOwner = (o.getCustomer() != null && current != null && Objects.equals(o.getCustomer().getUserId(), current.getUserId()));
        String roleName = current != null && current.getRole() != null ? current.getRole().getName() : null;
        boolean isStaff = roleName != null && (
                roleName.equalsIgnoreCase("ADMIN") ||
                roleName.equalsIgnoreCase("STAFF") ||
                roleName.equalsIgnoreCase("EMPLOYEE")
        );
        if (!isOwner && !isStaff) {
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

        String deliveryName = o.getAddress() != null ? "Shipping" : (o.getTable() != null ? ("Table " + o.getTable().getNumber()) : "");
        detail.put("delivery_name", deliveryName);
        detail.put("delivery_fee", defaultBigDecimal(o.getShippingAmount()));
        detail.put("grand_total", defaultBigDecimal(o.getTotalAmount()));
        detail.put("subtotal", defaultBigDecimal(o.getSubtotalAmount()));
        detail.put("discount", defaultBigDecimal(o.getDiscountAmount()));

        List<Map<String, Object>> products = new ArrayList<>();
        if (o.getOrderDetails() != null) {
            for (OrderDetail d : o.getOrderDetails()) {
                Product p = d.getProduct();
                Map<String, Object> pd = new HashMap<>();
                pd.put("id", d.getOrderDetailId());
                pd.put("product_name", d.getProductNameSnapshot() != null ? d.getProductNameSnapshot() : (p != null ? p.getName() : null));
                pd.put("product_img", p != null ? p.getImgUrl() : null);
                pd.put("qty", d.getQuantity());
                pd.put("size", d.getSize() != null ? d.getSize().getName() : null);
                pd.put("subtotal", defaultBigDecimal(d.getLineTotal()));
                List<Map<String, Object>> addOns = new ArrayList<>();
                for (OrderDetailAddOn oda : orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(d.getOrderDetailId())) {
                    Map<String, Object> ao = new HashMap<>();
                    ao.put("name", oda.getAddOn() != null ? oda.getAddOn().getName() : null);
                    ao.put("price", defaultBigDecimal(oda.getUnitPriceSnapshot()));
                    addOns.add(ao);
                }
                pd.put("add_ons", addOns);
                products.add(pd);
            }
        }
        detail.put("products", products);

        return ResponseEntity.ok(Map.of("data", List.of(detail)));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listPending(String status, String type, Integer page, Integer limit) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), Math.max(limit, 1));
        Page<OrderEntity> pageData;
        if ("TABLE".equalsIgnoreCase(type)) {
            // Show dine-in/table orders with NO delivery address and status NOT IN (CANCELLED, REJECTED, COMPLETED)
            pageData = orderRepository.findByStatusNotInAndAddressIsNull(List.of("CANCELLED", "REJECTED", "COMPLETED"), pageable);
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
			// Include payment method and status for UI decisions (e.g., shipper button label)
			if (o.getPayments() != null && !o.getPayments().isEmpty()) {
				Payment p = o.getPayments().get(0);
				m.put("payment_method", p.getMethod() != null ? p.getMethod().name() : null);
				m.put("payment_status", p.getStatus() != null ? p.getStatus().name() : null);
			} else {
				m.put("payment_method", null);
				m.put("payment_status", null);
			}
            m.put("created_at", o.getCreatedAt());
            m.put("table_number", o.getTable() != null ? o.getTable().getNumber() : null);
            m.put("total", defaultBigDecimal(o.getTotalAmount()));
            m.put("subtotal", defaultBigDecimal(o.getSubtotalAmount()));
            m.put("shipping_fee", defaultBigDecimal(o.getShippingAmount()));
            m.put("discount", defaultBigDecimal(o.getDiscountAmount()));
            m.put("shipper_id", o.getShipper() != null ? o.getShipper().getUserId() : null);

            List<Map<String, Object>> products = new ArrayList<>();
            if (o.getOrderDetails() != null) {
                for (OrderDetail d : o.getOrderDetails()) {
                    Product prod = d.getProduct();
                    Map<String, Object> pd = new HashMap<>();
                    pd.put("product_name", d.getProductNameSnapshot());
                    pd.put("product_img", prod != null ? prod.getImgUrl() : null);
                    pd.put("qty", d.getQuantity());
                    pd.put("subtotal", defaultBigDecimal(d.getLineTotal()));
                    pd.put("size", d.getSize() != null ? d.getSize().getName() : null);
                    List<Map<String, Object>> addOns = new ArrayList<>();
                    for (OrderDetailAddOn oda : orderDetailAddOnRepository.findByOrderDetail_OrderDetailId(d.getOrderDetailId())) {
                        Map<String, Object> ao = new HashMap<>();
                        ao.put("name", oda.getAddOn() != null ? oda.getAddOn().getName() : null);
                        ao.put("price", defaultBigDecimal(oda.getUnitPriceSnapshot()));
                        addOns.add(ao);
                    }
                    pd.put("add_ons", addOns);
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
                if (payment.getPaidAt() == null) payment.setPaidAt(LocalDateTime.now());
            } else if (Objects.equals(status, "CANCELLED")) {
                payment.setStatus(PaymentStatus.CANCELLED);
            } else if (Objects.equals(status, "PENDING")) {
                payment.setStatus(PaymentStatus.PENDING);
            } else if (Objects.equals(status, "PAID")) {
                payment.setStatus(PaymentStatus.PAID);
                if (payment.getPaidAt() == null) payment.setPaidAt(LocalDateTime.now());
            }
        }
        orderRepository.save(order);
        if (order.getTable() != null) {
            tableService.setAvailableIfNoPendingOrders(order.getTable().getTableId());
        }
        
        // Gửi thông báo khi status thay đổi (chỉ khi status thực sự thay đổi)
        if (!Objects.equals(previousStatus, status)) {
            try {
                // Gửi thông báo cho customer
                notificationService.notifyOrderStatusChangeToCustomer(order, previousStatus, status);
                // Gửi thông báo cho staff
                notificationService.notifyOrderStatusChangeToStaff(order, previousStatus, status);
            } catch (Exception ex) {
                // Không để lỗi thông báo làm vỡ flow thay đổi status
                org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn("Failed to send notifications for order {} status change: {}", 
                        order.getOrderId(), ex.getMessage());
            }
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
        Integer addressIdFromBody = null;
        if (body.containsKey("address_id") && body.get("address_id") != null) {
            try { addressIdFromBody = Integer.valueOf(String.valueOf(body.get("address_id"))); } catch (Exception ignored) {}
        }
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

            // Parse size_id (optional)
            Integer sizeId = null;
            if (p.containsKey("size_id") && p.get("size_id") != null) {
                try { sizeId = Integer.valueOf(String.valueOf(p.get("size_id"))); } catch (Exception ignored) {}
            }
            // Parse add_on_ids (array) or add_ons (array of ids or objects)
            List<Integer> addOnIds = parseAddOnIds(p.get("add_on_ids"), p.get("add_ons"));

            BigDecimal base = getDiscountedBasePrice(prod);
            BigDecimal sizeDelta = BigDecimal.ZERO;
            Size sizeEntity = null;
            if (sizeId != null) {
                sizeEntity = sizeRepository.findById(sizeId).orElse(null);
                var psOpt = productSizeRepository.findByProduct_ProductIdAndSize_SizeId(productId, sizeId);
                if (psOpt.isPresent() && psOpt.get().getPrice() != null) sizeDelta = psOpt.get().getPrice();
            }
            BigDecimal addOnSum = BigDecimal.ZERO;
            for (Integer aId : addOnIds) {
                var paOpt = productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(productId, aId);
                if (paOpt.isPresent() && paOpt.get().getPrice() != null) addOnSum = addOnSum.add(paOpt.get().getPrice());
            }
            BigDecimal unit = base.add(sizeDelta).add(addOnSum);

            subtotal = subtotal.add(unit.multiply(BigDecimal.valueOf(qty)));
            OrderDetail d = OrderDetail.builder()
                .product(prod)
                .productNameSnapshot(prod.getName())
                .unitPrice(unit)
                .quantity(qty)
                .size(sizeEntity)
                .build();
            details.add(d);
        }

        Address addr = null;
        User customer = null;
        // Determine flow: customer (role_id = 3) vs staff (role_id = 2)
        if (roleId != null && roleId == 3) {
            // Customer placing order: customer = current user, created_by = current user, address optional
            customer = userRepository.findById(userId).orElse(null);
            if (addressIdFromBody != null) {
                Address candidate = addressRepository.findById(addressIdFromBody).orElse(null);
                // Optional: ensure the address belongs to this user
                if (candidate != null) addr = candidate;
            } else if (addressText != null && !addressText.isBlank()) {
                // Always create a new ad-hoc address for this order when explicit address text provided
                addr = Address.builder()
                        .addressLine(addressText)
                        .createdAt(LocalDateTime.now())
                        .build();
                addr = addressRepository.save(addr);
            }
        } else if (roleId != null && roleId == 2) {
            // Staff placing order: if customer_id is provided, attach customer; if null, keep null; address must be null when customer is null
            if (customerIdFromBody != null) {
                customer = userRepository.findById(customerIdFromBody).orElse(null);
                if (addressIdFromBody != null) {
                    Address candidate = addressRepository.findById(addressIdFromBody).orElse(null);
                    if (candidate != null) addr = candidate;
                } else if (addressText != null && !addressText.isBlank()) {
                    addr = Address.builder()
                            .addressLine(addressText)
                            .createdAt(LocalDateTime.now())
                            .build();
                    addr = addressRepository.save(addr);
                }
            }
        } else {
            // Fallback: treat like customer, but keep safe
            customer = userRepository.findById(userId).orElse(null);
            if (addressIdFromBody != null) {
                Address candidate = addressRepository.findById(addressIdFromBody).orElse(null);
                if (candidate != null) addr = candidate;
            } else if (addressText != null && !addressText.isBlank()) {
                addr = Address.builder()
                        .addressLine(addressText)
                        .createdAt(LocalDateTime.now())
                        .build();
                addr = addressRepository.save(addr);
            }
        }

        // If shipping (address present), validate city and compute shipping fee with Mapbox
        BigDecimal shippingFee = BigDecimal.ZERO;
        if (addr != null) {
            // Ensure we have city and coordinates
            String city = addr.getCity();
            if (city == null || city.isBlank() || addr.getLatitude() == null || addr.getLongitude() == null) {
                var geo = mapboxService.geocodeAddress(addr.getAddressLine());
                if (geo != null) {
                    if (addr.getLatitude() == null) addr.setLatitude(geo.lat());
                    if (addr.getLongitude() == null) addr.setLongitude(geo.lng());
                    if (city == null || city.isBlank()) addr.setCity(geo.city());
                    addressRepository.save(addr);
                    city = addr.getCity();
                }
            }
            String normCity = normalizeCity(city);
            if (normCity == null || !normCity.equals("da nang")) {
                return ResponseEntity.badRequest().body(Map.of("message", "Chỉ có thể ship nội tỉnh (Đà Nẵng)"));
            }
            if (addr.getLatitude() == null || addr.getLongitude() == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Thiếu toạ độ giao hàng"));
            }
            double meters = mapboxService.getDrivingDistanceMeters(mapboxService.getShopLat(), mapboxService.getShopLng(), addr.getLatitude(), addr.getLongitude());
            if (meters < 0) {
                return ResponseEntity.status(502).body(Map.of("message", "Không tính được khoảng cách giao hàng"));
            }
            double km = meters / 1000.0;
            if (km < 1.0) shippingFee = BigDecimal.ZERO;
            else if (km < 3.0) shippingFee = new BigDecimal("10000");
            else if (km < 5.0) shippingFee = new BigDecimal("20000");
            else shippingFee = new BigDecimal("30000");
        }

        // Discount: prefer validating discount_code; fallback to provided discount_amount (validated)
        BigDecimal discount = BigDecimal.ZERO;
        String discountCodeStr = null;
        if (body.containsKey("discount_code") && body.get("discount_code") != null) {
            discountCodeStr = String.valueOf(body.get("discount_code")).trim();
        }
        DiscountCode appliedCode = null;
        if (discountCodeStr != null && !discountCodeStr.isBlank()) {
            var dcOpt = discountCodeRepository.findByCodeIgnoreCase(discountCodeStr);
            if (dcOpt.isPresent()) {
                DiscountCode dc = dcOpt.get();
                String validationError = validateDiscountCodeForUser(dc, subtotal, current);
                if (validationError == null) {
                    BigDecimal discountBase = Boolean.TRUE.equals(dc.getShippingFee()) ? shippingFee : subtotal;
                    discount = computeDiscountAmount(dc, discountBase);
                    appliedCode = dc;
                } else {
                    return ResponseEntity.badRequest().body(Map.of("message", validationError));
                }
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Mã giảm giá không tồn tại"));
            }
        } else {
            if (body.containsKey("discount_amount") && body.get("discount_amount") != null) {
                try { discount = new BigDecimal(String.valueOf(body.get("discount_amount"))); } catch (Exception ignored) {}
            }
            if (discount.compareTo(BigDecimal.ZERO) < 0) discount = BigDecimal.ZERO;
            if (discount.compareTo(subtotal) > 0) discount = subtotal;
        }

        OrderEntity order = OrderEntity.builder()
                .orderCode("ORD-" + System.currentTimeMillis())
                .status(paid ? "COMPLETED" : "PENDING")
                .subtotalAmount(subtotal)
                .shippingAmount(shippingFee)
                .discountAmount(discount)
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
                    .amount(subtotal.subtract(discount).add(shippingFee))
                    .method(method)
                    .status(paid ? PaymentStatus.PAID : PaymentStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            if (paid) {
                payment.setPaidAt(LocalDateTime.now());
            }
            order.getPayments().add(payment);
        }

        OrderEntity saved = orderRepository.save(order);
        // Ensure detail IDs are generated before inserting add-ons
        try { entityManager.flush(); } catch (Exception ignored) {}
        // New: persist add-ons by matching request products to saved details
        persistAddOnsForOrder(saved, products);
        // Record discount redemption if applied
        if (appliedCode != null) {
            DiscountCodeRedemption redemption = DiscountCodeRedemption.builder()
                    .discountCode(appliedCode)
                    .order(saved)
                    .user(current)
                    .redeemedAt(LocalDateTime.now())
                    .build();
            discountCodeRedemptionRepository.save(redemption);
            // increment usage count safely
            Integer usage = appliedCode.getUsageCount() == null ? 0 : appliedCode.getUsageCount();
            appliedCode.setUsageCount(usage + 1);
            discountCodeRepository.save(appliedCode);
        }
        return ResponseEntity.ok(Map.of("message", "OK", "data", Map.of("id", saved.getOrderId())));
    }

    private BigDecimal computeDiscountAmount(DiscountCode dc, BigDecimal base) {
        if (dc == null || base == null) return BigDecimal.ZERO;
        if (dc.getDiscountType() == com.kopi.kopi.entity.enums.DiscountType.PERCENT) {
            BigDecimal percent = dc.getDiscountValue() == null ? BigDecimal.ZERO : dc.getDiscountValue();
            BigDecimal amt = base.multiply(percent).divide(new BigDecimal("100"));
            if (amt.compareTo(base) > 0) amt = base;
            if (amt.compareTo(BigDecimal.ZERO) < 0) amt = BigDecimal.ZERO;
            return amt;
        }
        BigDecimal val = dc.getDiscountValue() == null ? BigDecimal.ZERO : dc.getDiscountValue();
        if (val.compareTo(base) > 0) val = base;
        if (val.compareTo(BigDecimal.ZERO) < 0) val = BigDecimal.ZERO;
        return val;
    }

    private String validateDiscountCodeForUser(DiscountCode dc, BigDecimal subtotal, User user) {
        if (dc == null) return "Mã giảm giá không hợp lệ";
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (!Boolean.TRUE.equals(dc.getActive())) return "Mã giảm giá đã bị vô hiệu hoá";
        if (dc.getStartsAt() != null && now.isBefore(dc.getStartsAt())) return "Mã giảm giá chưa bắt đầu";
        if (dc.getEndsAt() != null && now.isAfter(dc.getEndsAt())) return "Mã giảm giá đã hết hạn";
        if (dc.getMinOrderAmount() != null && subtotal != null && subtotal.compareTo(dc.getMinOrderAmount()) < 0) return "Chưa đạt giá trị đơn tối thiểu";
        if (dc.getTotalUsageLimit() != null) {
            int totalUsed = discountCodeRedemptionRepository.countByDiscountCode_DiscountCodeId(dc.getDiscountCodeId());
            if (totalUsed >= dc.getTotalUsageLimit()) return "Mã giảm giá đã đạt giới hạn sử dụng";
        }
        if (dc.getPerUserLimit() != null && user != null) {
            int perUser = discountCodeRedemptionRepository.countByDiscountCode_DiscountCodeIdAndUser_UserId(dc.getDiscountCodeId(), user.getUserId());
            if (perUser >= dc.getPerUserLimit()) return "Bạn đã dùng hết số lần cho mã này";
        }
        return null;
    }

    @Override
    public ResponseEntity<?> validateDiscount(Map<String, Object> body, User current) {
        String code = body == null ? null : String.valueOf(body.getOrDefault("code", "")).trim();
        BigDecimal subtotal = BigDecimal.ZERO;
        if (body != null && body.get("subtotal") != null) {
            try { subtotal = new BigDecimal(String.valueOf(body.get("subtotal"))); } catch (Exception ignored) {}
        }
        BigDecimal shipping = BigDecimal.ZERO;
        if (body != null && body.get("shipping") != null) {
            try { shipping = new BigDecimal(String.valueOf(body.get("shipping"))); } catch (Exception ignored) {}
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập mã giảm giá"));
        }
        var dcOpt = discountCodeRepository.findByCodeIgnoreCase(code);
        if (dcOpt.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Mã giảm giá không tồn tại"));
        }
        DiscountCode dc = dcOpt.get();
        String error = validateDiscountCodeForUser(dc, subtotal, current);
        if (error != null) {
            return ResponseEntity.badRequest().body(Map.of("message", error));
        }
        BigDecimal base = Boolean.TRUE.equals(dc.getShippingFee()) ? (shipping != null ? shipping : BigDecimal.ZERO) : subtotal;
        BigDecimal amount = computeDiscountAmount(dc, base);
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "discount_amount", amount,
                "coupon_code", dc.getCode(),
                "discount_type", dc.getDiscountType() != null ? dc.getDiscountType().name() : null,
                "discount_value", dc.getDiscountValue(),
                "applies_to_shipping", Boolean.TRUE.equals(dc.getShippingFee()),
                "message", "Áp dụng mã giảm giá thành công"
        ));
    }

    private List<Integer> parseAddOnIds(Object primary, Object fallback) {
        List<Integer> ids = new ArrayList<>();
        Object raw = primary != null ? primary : fallback;
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o == null) continue;
                if (o instanceof Number n) ids.add(n.intValue());
                else if (o instanceof Map<?, ?> om) {
                    Object idVal = om.get("id");
                    if (idVal == null) idVal = om.get("add_on_id");
                    if (idVal instanceof Number n2) ids.add(n2.intValue());
                    else if (idVal != null) {
                        try { ids.add(Integer.valueOf(String.valueOf(idVal))); } catch (Exception ignored) {}
                    }
                } else {
                    try { ids.add(Integer.valueOf(String.valueOf(o))); } catch (Exception ignored) {}
                }
            }
        }
        return ids;
    }

    // Persist order_detail_add_ons by matching each request product to a saved detail
    private void persistAddOnsForOrder(OrderEntity order, List<Map<String, Object>> requestProducts) {
        if (order == null || requestProducts == null || requestProducts.isEmpty()) return;
        List<OrderDetail> details = order.getOrderDetails() == null ? List.of() : new ArrayList<>(order.getOrderDetails());
        boolean[] used = new boolean[details.size()];
        List<OrderDetailAddOn> toSave = new ArrayList<>();
        for (Map<String, Object> p : requestProducts) {
            Integer productId;
            Integer qty;
            try { productId = Integer.valueOf(String.valueOf(p.get("product_id"))); } catch (Exception e) { continue; }
            try { qty = Integer.valueOf(String.valueOf(p.getOrDefault("qty", 1))); } catch (Exception e) { qty = 1; }
            Integer sizeId = null;
            if (p.containsKey("size_id") && p.get("size_id") != null) {
                try { sizeId = Integer.valueOf(String.valueOf(p.get("size_id"))); } catch (Exception ignored) {}
            }
            List<Integer> addOnIds = parseAddOnIds(p.get("add_on_ids"), p.get("add_ons"));
            if (addOnIds.isEmpty()) continue;

            // Compute expected unit price again to match detail when duplicates exist
            Product prod = productRepository.findById(productId).orElse(null);
            if (prod == null) continue;
            BigDecimal base = getDiscountedBasePrice(prod);
            BigDecimal sizeDelta = BigDecimal.ZERO;
            if (sizeId != null) {
                var ps = productSizeRepository.findByProduct_ProductIdAndSize_SizeId(productId, sizeId).orElse(null);
                if (ps != null && ps.getPrice() != null) sizeDelta = ps.getPrice();
            }
            BigDecimal addOnSum = BigDecimal.ZERO;
            for (Integer aId : addOnIds) {
                var pa = productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(productId, aId).orElse(null);
                if (pa != null && pa.getPrice() != null) addOnSum = addOnSum.add(pa.getPrice());
            }
            BigDecimal expectedUnit = base.add(sizeDelta).add(addOnSum);

            // Find first unmatched detail that matches all fields
            int matchIdx = -1;
            for (int i = 0; i < details.size(); i++) {
                if (used[i]) continue;
                OrderDetail d = details.get(i);
                if (d.getProduct() == null || d.getProduct().getProductId() == null) continue;
                if (!Objects.equals(d.getProduct().getProductId(), productId)) continue;
                if (!Objects.equals(d.getQuantity(), qty)) continue;
                Integer dSizeId = d.getSize() != null ? d.getSize().getSizeId() : null;
                if (!Objects.equals(dSizeId, sizeId)) continue;
                BigDecimal dUnit = d.getUnitPrice() != null ? d.getUnitPrice() : BigDecimal.ZERO;
                if (dUnit.compareTo(expectedUnit) != 0) continue;
                matchIdx = i;
                break;
            }
            if (matchIdx == -1) continue;
            used[matchIdx] = true;
            OrderDetail matched = details.get(matchIdx);
            for (Integer aId : addOnIds) {
                var paOpt = productAddOnRepository.findByProduct_ProductIdAndAddOn_AddOnId(productId, aId);
                if (paOpt.isEmpty()) continue;
                ProductAddOn pa = paOpt.get();
                toSave.add(OrderDetailAddOn.builder()
                        .orderDetail(matched)
                        .addOn(pa.getAddOn())
                        .unitPriceSnapshot(pa.getPrice() != null ? pa.getPrice() : BigDecimal.ZERO)
                        .build());
            }
        }
        if (!toSave.isEmpty()) orderDetailAddOnRepository.saveAll(toSave);
    }

    private String normalizeCity(String s) {
        if (s == null) return null;
        String lower = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
        return lower.trim();
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
            BigDecimal unit = getDiscountedBasePrice(prod);
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
                .shippingAmount(BigDecimal.ZERO)
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
        if (Boolean.TRUE.equals(req.paid())) {
            payment.setPaidAt(LocalDateTime.now());
        }
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
