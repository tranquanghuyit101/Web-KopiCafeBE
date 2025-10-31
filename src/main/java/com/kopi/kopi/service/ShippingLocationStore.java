package com.kopi.kopi.service;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ShippingLocationStore {
    public static class Location {
        public Double lat;
        public Double lng;
        public LocalDateTime updatedAt;
        public Integer byUserId;
        public Location(Double lat, Double lng, Integer byUserId) {
            this.lat = lat;
            this.lng = lng;
            this.byUserId = byUserId;
            this.updatedAt = LocalDateTime.now();
        }
    }

    private final Map<Integer, Location> store = new ConcurrentHashMap<>();

    public void put(Integer orderId, Double lat, Double lng, Integer byUserId) {
        store.put(orderId, new Location(lat, lng, byUserId));
    }

    public Location get(Integer orderId) {
        return store.get(orderId);
    }
}


