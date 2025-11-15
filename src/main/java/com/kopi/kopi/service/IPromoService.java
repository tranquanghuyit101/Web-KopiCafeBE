package com.kopi.kopi.service;

import java.util.Map;

import com.kopi.kopi.dto.promo.CreateCodeDTO;
import com.kopi.kopi.dto.promo.CreateEventDTO;
import com.kopi.kopi.dto.promo.PromoDetailDTO;
import com.kopi.kopi.dto.promo.UpdatePromoDTO;
import com.kopi.kopi.entity.DiscountCode;
import com.kopi.kopi.entity.DiscountEvent;

public interface IPromoService {
    Map<String, Object> list(Integer page, Integer limit, String available, String status, String searchByName);
    DiscountCode create(CreateCodeDTO body);
    DiscountEvent createEvent(CreateEventDTO body);
    PromoDetailDTO getOne(Integer id);
    void update(Integer id, UpdatePromoDTO body);
    void softDelete(Integer id);
}


