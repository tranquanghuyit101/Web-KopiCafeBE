package com.kopi.kopi.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class MapboxService {
    private final RestTemplate http = new RestTemplate();
    private final ObjectMapper om = new ObjectMapper();

    @Value("${app.mapbox.token:${MAPBOX_TOKEN:}}")
    private String mapboxToken;

    @Value("${app.shop.lat:${APP_SHOP_LAT:16.047079}}")
    private double shopLat;

    @Value("${app.shop.lng:${APP_SHOP_LNG:108.206230}}")
    private double shopLng;

    public record GeoResult(Double lat, Double lng, String city) {}

    public double getDrivingDistanceMeters(double fromLat, double fromLng, double toLat, double toLng) {
        try {
            if (mapboxToken == null || mapboxToken.isBlank()) {
                throw new IllegalStateException("Missing Mapbox token. Set app.mapbox.token or MAPBOX_TOKEN");
            }
            String coords = "%f,%f;%f,%f".formatted(fromLng, fromLat, toLng, toLat);
            String url = "https://api.mapbox.com/directions/v5/mapbox/driving/" + coords
                    + "?alternatives=false&geometries=geojson&overview=false&access_token=" + mapboxToken;
            ResponseEntity<String> r = http.getForEntity(url, String.class);
            if (!r.getStatusCode().is2xxSuccessful()) return -1;
            JsonNode root = om.readTree(r.getBody());
            JsonNode routes = root.path("routes");
            if (!routes.isArray() || routes.isEmpty()) return -1;
            return routes.get(0).path("distance").asDouble(-1);
        } catch (Exception e) {
            return -1;
        }
    }

    public GeoResult geocodeAddress(String address) {
        try {
            if (mapboxToken == null || mapboxToken.isBlank()) {
                throw new IllegalStateException("Missing Mapbox token. Set app.mapbox.token or MAPBOX_TOKEN");
            }
            String q = URLEncoder.encode(address == null ? "" : address, StandardCharsets.UTF_8);
            String url = "https://api.mapbox.com/geocoding/v5/mapbox.places/" + q + ".json"
                    + "?limit=1&autocomplete=true&country=VN&access_token=" + mapboxToken;
            ResponseEntity<String> r = http.getForEntity(url, String.class);
            if (!r.getStatusCode().is2xxSuccessful()) return null;
            JsonNode root = om.readTree(r.getBody());
            JsonNode features = root.path("features");
            if (!features.isArray() || features.isEmpty()) return null;
            JsonNode f = features.get(0);
            JsonNode center = f.path("center");
            Double lng = center.get(0).asDouble();
            Double lat = center.get(1).asDouble();
            String city = null;
            if (f.has("context") && f.get("context").isArray()) {
                for (JsonNode c : f.get("context")) {
                    String id = c.path("id").asText("");
                    if (id.startsWith("place")) {
                        city = c.path("text").asText(null);
                    }
                }
            }
            if (city == null) {
                // fallback to place_name token near the end
                String placeName = f.path("place_name").asText("");
                String[] tokens = placeName.split(",");
                if (tokens.length >= 2) city = tokens[Math.max(0, tokens.length - 2)].trim();
            }
            return new GeoResult(lat, lng, city);
        } catch (Exception e) {
            return null;
        }
    }

    public double getShopLat() { return shopLat; }
    public double getShopLng() { return shopLng; }
}


