package com.kopi.kopi.controller;

import com.kopi.kopi.payload.request.OrderCompletedRequest;
import com.kopi.kopi.payload.request.OrderPaidRequest;
import com.kopi.kopi.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
public class NotificationEventController {

    private final INotificationService notify;

    @PostMapping("/order/paid")
    public ResponseEntity<Void> orderPaid(@RequestBody OrderPaidRequest p) {
        notify.onOrderPaid(p.getOrderId(), p.getCustomerId(), p.getStaffId(), p.getOrderCode());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/order/completed")
    public ResponseEntity<Void> orderCompleted(@RequestBody OrderCompletedRequest p) {
        notify.onOrderCompleted(p.getOrderId(), p.getCustomerId(), p.getOrderCode());
        return ResponseEntity.noContent().build();
    }
}
