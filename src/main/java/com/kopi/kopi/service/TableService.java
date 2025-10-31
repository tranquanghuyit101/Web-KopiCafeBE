package com.kopi.kopi.service;

import org.springframework.http.ResponseEntity;

import java.util.Map;

public interface TableService {
    void setOccupiedIfHasPendingOrders(Integer tableId);
    void setAvailableIfNoPendingOrders(Integer tableId);
    Map<String, Object> list(Integer page, Integer limit, String status);
    ResponseEntity<?> create(Map<String, Object> body);
    ResponseEntity<?> patch(Integer id, Map<String, Object> body);
    ResponseEntity<?> delete(Integer id);
    ResponseEntity<?> rotateQr(Integer id);
    ResponseEntity<?> getByQr(String qrToken);
}


