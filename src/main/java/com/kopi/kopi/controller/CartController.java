package com.kopi.kopi.controller;

import com.kopi.kopi.entity.OrderDetail;
import com.kopi.kopi.entity.OrderEntity;
import com.kopi.kopi.entity.Product;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.entity.enums.OrderStatus;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.repository.ProductRepository;
import com.kopi.kopi.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/apiv1/cart")
public class CartController {

    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;

    public CartController(OrderRepository orderRepo, ProductRepository productRepo) {
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@RequestParam Integer productId,
                                       @RequestParam int quantity,
                                       Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = principal.getUser();

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        // Validate trạng thái và tồn kho
        if (!Boolean.TRUE.equals(product.getAvailable()) && quantity > product.getStockQty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Không đủ tồn kho, chỉ còn " + product.getStockQty() + " sản phẩm")
            );
        }

        // Tìm order CART của user
        OrderEntity cart = orderRepo.findByCustomerAndStatus(user, OrderStatus.CART)
                .orElseGet(() -> {
                    OrderEntity newCart = new OrderEntity();
                    newCart.setCustomer(user);
                    newCart.setStatus(OrderStatus.CART);
                    return orderRepo.save(newCart);
                });

        // Kiểm tra sản phẩm đã có trong cart chưa
        Optional<OrderDetail> existingDetail = cart.getOrderDetails().stream()
                .filter(od -> od.getProduct().getProductId().equals(productId))
                .findFirst();

        if (existingDetail.isPresent()) {
            int currentQty = existingDetail.get().getQuantity();
            int nextQty = currentQty + quantity;
            if (nextQty > product.getStockQty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Không đủ tồn kho, chỉ còn " + product.getStockQty() + " sản phẩm")
                );
            }
            existingDetail.get().setQuantity(nextQty);
        } else {
            OrderDetail newDetail = new OrderDetail();
            newDetail.setOrder(cart);
            newDetail.setProduct(product);
            newDetail.setQuantity(quantity);
            cart.getOrderDetails().add(newDetail);
        }

        orderRepo.save(cart);
        return ResponseEntity.ok(Map.of("message", "Thêm vào giỏ hàng thành công"));
    }

    @PutMapping("/updateQuantity")
    public ResponseEntity<?> updateCart(@RequestParam Integer productId,
                                        @RequestParam int quantity,
                                        Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = principal.getUser();

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        if (!Boolean.TRUE.equals(product.getAvailable()) || quantity > product.getStockQty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Không đủ tồn kho, chỉ còn " + product.getStockQty() + " sản phẩm")
            );
        }

        OrderEntity cart = orderRepo.findByCustomerAndStatus(user, OrderStatus.CART)
                .orElse(null);
        if (cart == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Giỏ hàng không tồn tại"));
        }

        Optional<OrderDetail> existingDetail = cart.getOrderDetails().stream()
                .filter(od -> od.getProduct().getProductId().equals(productId))
                .findFirst();

        if (existingDetail.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sản phẩm chưa có trong giỏ hàng"));
        }

        existingDetail.get().setQuantity(quantity);
        orderRepo.save(cart);
        return ResponseEntity.ok(Map.of("message", "Cập nhật số lượng thành công"));
    }

    @GetMapping
    public ResponseEntity<?> viewCart(Authentication auth) {
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = principal.getUser();

        OrderEntity cart = orderRepo.findByCustomerAndStatus(user, OrderStatus.CART).orElse(null);
        return ResponseEntity.ok(cart);
    }
}
