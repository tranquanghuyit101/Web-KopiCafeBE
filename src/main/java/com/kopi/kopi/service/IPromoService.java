package com.kopi.kopi.service;

import java.util.Map;

public interface IPromoService {
    Map<String, Object> list(Integer page, Integer limit, String available, String status, String searchByName);
}


