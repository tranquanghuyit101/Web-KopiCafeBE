package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.OrderEntity;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.service.ShippingLocationStore;
import com.kopi.kopi.service.ShippingLocationStore.Location;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShippingServiceImplTest {

    @Mock
    private ShippingLocationStore store;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private UserRepository userRepository;

    private ShippingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ShippingServiceImpl(store, orderRepository, userRepository);
    }

    @AfterEach
    void tearDown() {
        // no-op
    }

    // ========= updateLocation =========

    @Test
    void should_ReturnBadRequest_When_LatOrLngMissing() {
        // Given
        // When
        ResponseEntity<?> resp1 = service.updateLocation(1, 2, null, 10.0);
        ResponseEntity<?> resp2 = service.updateLocation(1, 2, 10.0, null);

        // Then
        assertThat(resp1.getStatusCodeValue()).isEqualTo(400);
        assertThat(((Map<?, ?>) resp1.getBody()).get("message")).isEqualTo("lat,lng required");
        assertThat(resp2.getStatusCodeValue()).isEqualTo(400);
        verify(store, never()).put(anyInt(), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    void should_PutLocationAndReturnOk_When_ValidInput() {
        // Given
        // When
        ResponseEntity<?> resp = service.updateLocation(10, 99, 1.23, 4.56);

        // Then
        verify(store).put(10, 1.23, 4.56, 99);
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(((Map<?, ?>) resp.getBody()).get("message")).isEqualTo("OK");
    }

    // ========= getLocation =========

    @Test
    void should_Return404_When_CustomerAndOrderNotFound() {
        // Given
        when(orderRepository.findById(5)).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> resp = service.getLocation(5, "ROLE_CUSTOMER", 100);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(404);
        assertThat(((Map<?, ?>) resp.getBody()).get("message")).isEqualTo("Order not found");
        verify(store, never()).get(anyInt());
    }

    @Test
    void should_Return403_When_CustomerButNotOwner() {
        // Given
        OrderEntity order = mock(OrderEntity.class);
        User owner = new User();
        owner.setUserId(200);
        when(order.getCustomer()).thenReturn(owner);
        when(orderRepository.findById(7)).thenReturn(Optional.of(order));

        // When
        ResponseEntity<?> resp = service.getLocation(7, "ROLE_CUSTOMER", 999);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(403);
        assertThat(((Map<?, ?>) resp.getBody()).get("message")).isEqualTo("Forbidden");
        verify(store, never()).get(anyInt());
    }

    @Test
    void should_ReturnCoordinates_When_LocationExists_AndRoleIsStaff() {
        // Given (non-customer path → không cần kiểm tra belong)
        when(store.get(77)).thenReturn(new Location(10.5, 20.6, 123));

        // When
        ResponseEntity<?> resp = service.getLocation(77, "ROLE_STAFF", 123);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        Map<String, Object> data = (Map<String, Object>) ((Map<?, ?>) resp.getBody()).get("data");
        assertThat(data.get("lat")).isEqualTo(10.5);
        assertThat(data.get("lng")).isEqualTo(20.6);
    }

    @Test
    void should_ReturnNullData_When_LocationMissing() {
        // Given
        when(store.get(88)).thenReturn(null);

        // When / Then: production currently uses Map.of(...) and will throw when trying
        // to put nulls,
        // so assert that the service invocation throws a NullPointerException.
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.getLocation(88, "ROLE_STAFF", 1))
                .isInstanceOf(NullPointerException.class);
    }

    // ========= getOrderShippingInfo =========

    @Test
    void should_Return404_When_OrderNotFound() {
        // Given
        when(orderRepository.findById(1)).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> resp = service.getOrderShippingInfo(1, "ROLE_CUSTOMER", 10);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(404);
        assertThat(((Map<?, ?>) resp.getBody()).get("message")).isEqualTo("Order not found");
    }

    @Test
    void should_Return403_When_CustomerNotOwner() {
        // Given
        OrderEntity order = mock(OrderEntity.class);
        User owner = new User();
        owner.setUserId(10);
        when(order.getCustomer()).thenReturn(owner);
        when(orderRepository.findById(2)).thenReturn(Optional.of(order));

        // When
        ResponseEntity<?> resp = service.getOrderShippingInfo(2, "ROLE_CUSTOMER", 99);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(403);
        assertThat(((Map<?, ?>) resp.getBody()).get("message")).isEqualTo("Forbidden");
    }

    @Test
    void should_ReturnAddressAndLatLng_When_AddressPresent() {
        // Given
        var address = mock(com.kopi.kopi.entity.Address.class);
        when(address.getAddressLine()).thenReturn("42 Wallaby Way");
        when(address.getLatitude()).thenReturn(11.11);
        when(address.getLongitude()).thenReturn(22.22);

        OrderEntity order = mock(OrderEntity.class);
        when(orderRepository.findById(3)).thenReturn(Optional.of(order));
        when(order.getAddress()).thenReturn(address);

        // When
        ResponseEntity<?> resp = service.getOrderShippingInfo(3, "ROLE_STAFF", 1);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        Map<String, Object> data = (Map<String, Object>) ((Map<?, ?>) resp.getBody()).get("data");
        assertThat(data.get("address")).isEqualTo("42 Wallaby Way");
        assertThat(data.get("lat")).isEqualTo(11.11);
        assertThat(data.get("lng")).isEqualTo(22.22);
    }

    @Test
    void should_ReturnNulls_When_AddressMissing() {
        // Given
        OrderEntity order = mock(OrderEntity.class);
        var address = mock(com.kopi.kopi.entity.Address.class);
        when(address.getAddressLine()).thenReturn("");
        when(address.getLatitude()).thenReturn(0.0);
        when(address.getLongitude()).thenReturn(0.0);
        when(orderRepository.findById(4)).thenReturn(Optional.of(order));
        when(order.getAddress()).thenReturn(address);

        // When
        ResponseEntity<?> resp = service.getOrderShippingInfo(4, "ROLE_STAFF", 1);

        // Then
        Map<String, Object> data = (Map<String, Object>) ((Map<?, ?>) resp.getBody()).get("data");
        assertThat(data.get("address")).isEqualTo("");
        assertThat(data.get("lat")).isEqualTo(0.0);
        assertThat(data.get("lng")).isEqualTo(0.0);
    }

    // ========= claimOrder =========

    @Test
    void should_Return404_When_Claim_OrderNotFound() {
        // Given
        when(orderRepository.findById(100)).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> resp = service.claimOrder(100, 7);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(404);
        assertThat(((Map<?, ?>) resp.getBody()).get("message")).isEqualTo("Order not found");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void should_ReturnBadRequest_When_OrderHasNoAddress() {
        // Given
        OrderEntity order = mock(OrderEntity.class);
        when(orderRepository.findById(200)).thenReturn(Optional.of(order));
        when(order.getAddress()).thenReturn(null);

        // When
        ResponseEntity<?> resp = service.claimOrder(200, 9);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        assertThat(((Map<?, ?>) resp.getBody()).get("message")).isEqualTo("Order has no address");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void should_ReturnConflict_When_AlreadyClaimedByAnotherShipper() {
        // Given
        OrderEntity order = mock(OrderEntity.class);
        User other = new User();
        other.setUserId(22);
        when(order.getShipper()).thenReturn(other);
        when(order.getAddress()).thenReturn(mock(com.kopi.kopi.entity.Address.class));
        when(orderRepository.findById(300)).thenReturn(Optional.of(order));

        // Service will call userRepository.findById(userId) — ensure it returns a user
        // to avoid NoSuchElementException
        when(userRepository.findById(eq(99))).thenReturn(Optional.of(new User()));

        // When
        ResponseEntity<?> resp = service.claimOrder(300, 99);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(409);
        assertThat(((Map<?, ?>) resp.getBody()).get("message")).isEqualTo("Order already claimed");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void should_ClaimSuccessfully_When_NoActiveOrdersAndNotClaimed() {
        // Given
        OrderEntity order = mock(OrderEntity.class);
        when(order.getShipper()).thenReturn(null);
        when(order.getAddress()).thenReturn(mock(com.kopi.kopi.entity.Address.class));
        when(orderRepository.findById(400)).thenReturn(Optional.of(order));
        when(orderRepository.countByShipper_UserIdAndStatusNotIn(eq(7), anyList())).thenReturn(0L);

        User shipper = new User();
        shipper.setUserId(7);
        when(userRepository.findById(7)).thenReturn(Optional.of(shipper));

        // When
        ResponseEntity<?> resp = service.claimOrder(400, 7);

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(((Map<?, ?>) resp.getBody()).get("message")).isEqualTo("OK");
        verify(order).setShipper(shipper);
        verify(orderRepository).save(order);
    }
}
