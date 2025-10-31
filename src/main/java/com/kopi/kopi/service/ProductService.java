package com.kopi.kopi.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;

public interface ProductService {
    Map<String, Object> list(Integer categoryId, String orderBy, String sort, String searchByName, Integer limit, Integer page);
    Map<String, Object> detail(Integer id);
    ResponseEntity<?> create(MultipartFile image, String imgUrl, String name, Integer categoryId, String desc, BigDecimal price);
    ResponseEntity<?> update(Integer id, MultipartFile image, String imgUrl, String name, Integer categoryId, String desc, BigDecimal price);
    ResponseEntity<?> delete(Integer id);
}

