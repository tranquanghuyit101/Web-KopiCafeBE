package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.Address;
import com.kopi.kopi.entity.OrderEntity;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.service.ShippingLocationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

// import java.time.LocalDateTime; (not used)
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingServiceImplExtraTest {

    @Mock
    ShippingLocationStore store;
    @Mock
    OrderRepository orderRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    com.kopi.kopi.repository.AddressRepository addressRepository;
    @Mock
    MapboxService mapbox;

    @InjectMocks
    ShippingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ShippingServiceImpl(store, orderRepository, userRepository, addressRepository, mapbox);
    }

    @Test
    void updateLocation_missingLatOrLng_returnsBadRequest() {
        ResponseEntity<?> r = service.updateLocation(1, 2, null, 108.0);
        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(((Map<?, ?>) r.getBody()).get("message")).isEqualTo("lat,lng required");
    }

    @Test
    void updateLocation_withCoords_callsStorePut_andReturnsOk() {
        ResponseEntity<?> r = service.updateLocation(10, 11, 16.0, 108.0);
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(((Map<?, ?>) r.getBody()).get("message")).isEqualTo("OK");
        verify(store).put(10, 16.0, 108.0, 11);
    }

    @Test
    void getLocation_forbidden_whenNotCustomerOrShipper() {
        OrderEntity o = OrderEntity.builder().orderId(100).customer(User.builder().userId(1).build()).build();
        when(orderRepository.findById(100)).thenReturn(Optional.of(o));

        ResponseEntity<?> r = service.getLocation(100, "CUSTOMER", 2);
        assertThat(r.getStatusCode().value()).isEqualTo(403);
        assertThat(((Map<?, ?>) r.getBody()).get("message")).isEqualTo("Forbidden");
    }

    @Test
    void getLocation_ok_whenCustomer_andLocationExists() {
        User cust = User.builder().userId(77).build();
        OrderEntity o = OrderEntity.builder().orderId(200).customer(cust).build();
        when(orderRepository.findById(200)).thenReturn(Optional.of(o));
        ShippingLocationStore.Location loc = new ShippingLocationStore.Location(16.01, 108.2, 77);
        when(store.get(200)).thenReturn(loc);

        ResponseEntity<?> r = service.getLocation(200, "CUSTOMER", 77);
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> outer = (Map<?, ?>) r.getBody();
        Map<?, ?> data = (Map<?, ?>) outer.get("data");
        assertThat(((Number) data.get("lat")).doubleValue()).isEqualTo(16.01);
        assertThat(((Number) data.get("lng")).doubleValue()).isEqualTo(108.2);
    }

    @Test
    void claimOrder_fails_whenOrderHasNoAddress() {
        OrderEntity o = OrderEntity.builder().orderId(300).address(null).build();
        when(orderRepository.findById(300)).thenReturn(Optional.of(o));

        ResponseEntity<?> r = service.claimOrder(300, 5);
        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(((Map<?, ?>) r.getBody()).get("message")).isEqualTo("Order has no address");
    }

    @Test
    void claimOrder_success_assignsShipper_andSaves() {
        OrderEntity o = OrderEntity.builder().orderId(400).address(Address.builder().addressId(1).build()).shipper(null)
                .build();
        when(orderRepository.findById(400)).thenReturn(Optional.of(o));
        User u = User.builder().userId(9).build();
        when(userRepository.findById(9)).thenReturn(Optional.of(u));
        when(orderRepository.countByShipper_UserIdAndStatusNotIn(eq(9), any())).thenReturn(0L);

        ResponseEntity<?> r = service.claimOrder(400, 9);
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        assertThat(((Map<?, ?>) r.getBody()).get("message")).isEqualTo("OK");
        verify(orderRepository).save(any(OrderEntity.class));
    }

    @Test
    void estimateFee_requiresAddressIdOrLine() {
        ResponseEntity<?> r = service.estimateFee(null, null);
        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(((Map<?, ?>) r.getBody()).get("message")).isEqualTo("address_id hoặc address_line là bắt buộc");
    }

    @Test
    void estimateFee_geocodeFailure_returnsBadRequest() {
        when(mapbox.geocodeAddress("foo"))
                .thenReturn(null);
        ResponseEntity<?> r = service.estimateFee(null, "foo");
        assertThat(r.getStatusCode().value()).isEqualTo(400);
        assertThat(((Map<?, ?>) r.getBody()).get("message")).isEqualTo("Không xác định được địa chỉ");
    }

    @Test
    void estimateFee_nonDaNangCity_returns400() {
        MapboxService.GeoResult geo = new MapboxService.GeoResult(16.0, 108.0, "Hanoi");
        when(mapbox.geocodeAddress("x"))
                .thenReturn(geo);
        ResponseEntity<?> r = service.estimateFee(null, "x");
        assertThat(r.getStatusCodeValue()).isEqualTo(400);
        assertThat(((Map<?, ?>) r.getBody()).get("message")).isEqualTo("Chỉ có thể ship nội tỉnh (Đà Nẵng)");
    }

    @Test
    void estimateFee_negativeDistance_returns502() {
        MapboxService.GeoResult geo = new MapboxService.GeoResult(16.0, 108.0, "Da Nang");
        when(mapbox.geocodeAddress("y")).thenReturn(geo);
        when(mapbox.getDrivingDistanceMeters(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(-1.0);
        ResponseEntity<?> r = service.estimateFee(null, "y");
        assertThat(r.getStatusCode().value()).isEqualTo(502);
        assertThat(((Map<?, ?>) r.getBody()).get("message")).isEqualTo("Không tính được khoảng cách");
    }

    @Test
    void estimateFee_success_smallDistance_shippingFeeZero() {
        MapboxService.GeoResult geo = new MapboxService.GeoResult(16.0, 108.0, "Da Nang");
        when(mapbox.geocodeAddress("z")).thenReturn(geo);
        when(mapbox.getDrivingDistanceMeters(anyDouble(), anyDouble(), anyDouble(), anyDouble())).thenReturn(500.0);
        ResponseEntity<?> r = service.estimateFee(null, "z");
        assertThat(r.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> outer = (Map<?, ?>) r.getBody();
        Map<?, ?> data = (Map<?, ?>) outer.get("data");
        assertThat(((Number) data.get("distance_meters")).doubleValue()).isEqualTo(500.0);
        assertThat(((Number) data.get("shipping_fee")).longValue()).isEqualTo(0L);
    }
}
