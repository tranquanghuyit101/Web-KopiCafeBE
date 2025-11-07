package com.kopi.kopi.service;

import com.kopi.kopi.controller.GuestOrderController;
import com.kopi.kopi.entity.User;
import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface OrderService {
    Map<String, Object> getUserTransactions(Integer userId, Integer page, Integer limit);
    ResponseEntity<?> getTransactionDetail(Integer id, User current);
    Map<String, Object> listPending(String status, String type, Integer page, Integer limit);
    ResponseEntity<?> changeStatus(Integer id, Map<String, Object> payload);
    ResponseEntity<?> createTransaction(Map<String, Object> body, User current);
    ResponseEntity<?> createGuestTableOrder(GuestOrderController.GuestOrderRequest req);
    ResponseEntity<?> validateProducts(Map<String, Object> body);
    ResponseEntity<?> validateDiscount(Map<String, Object> body, User current);
}
