package com.kopi.kopi.controller;

import com.kopi.kopi.entity.OrderDetail;
import com.kopi.kopi.entity.OrderEntity;
import com.kopi.kopi.entity.Payment;
import com.kopi.kopi.entity.Product;
import com.kopi.kopi.repository.OrderRepository;
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

    public TransactionController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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

    private BigDecimal defaultBigDecimal(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}


