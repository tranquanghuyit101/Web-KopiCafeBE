package com.kopi.kopi.controller;
import java.util.List;
import com.kopi.kopi.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

 

@RestController
@RequestMapping("/apiv1/guest")
public class GuestOrderController {
    private final OrderService orderService;

    public GuestOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    public record GuestOrderItem(Integer product_id, Integer qty) {}
    public record GuestOrderRequest(String qr_token, Integer table_number, List<GuestOrderItem> products, String notes, Integer payment_id, Boolean paid) {}

    @PostMapping("/table-orders")
    @Transactional
    public ResponseEntity<?> createGuestTableOrder(@RequestBody GuestOrderRequest req) {
        return orderService.createGuestTableOrder(req);
    }
}


