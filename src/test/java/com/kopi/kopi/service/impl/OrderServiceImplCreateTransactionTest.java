package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.*;
import com.kopi.kopi.repository.*;
import com.kopi.kopi.service.TableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplCreateTransactionTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TableService tableService;
    @Mock
    private DiningTableRepository diningTableRepository;
    @Mock
    private UserAddressRepository userAddressRepository;
    @Mock
    private MapboxService mapboxService;

    @InjectMocks
    private OrderServiceImpl svc;

    private User buildUser(int userId, int roleId) {
        Role r = Role.builder().roleId(roleId).build();
        return User.builder().userId(userId).role(r).build();
    }

    @Test
    void createTransaction_cityNotDaNang_returnsBadRequest() {
        // product
        Product prod = Product.builder().productId(1).name("P").stockQty(10).price(new BigDecimal("100")).build();
        when(productRepository.findById(1)).thenReturn(Optional.of(prod));

        // address belongs to user
        Address addr = Address.builder().addressId(1).addressLine("Somewhere").city("Hanoi").latitude(16.0)
                .longitude(108.0).build();
        when(addressRepository.findById(1)).thenReturn(Optional.of(addr));

        User current = buildUser(10, 3); // role = 3 (customer)
        when(userRepository.findById(10)).thenReturn(Optional.of(current));

        Map<String, Object> body = new HashMap<>();
        body.put("products", List.of(Map.of("product_id", 1, "qty", 1)));
        body.put("address_id", 1);

        ResponseEntity<?> res = svc.createTransaction(body, current);
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        assertThat(((Map<?, ?>) res.getBody()).get("message")).isEqualTo("Chỉ có thể ship nội tỉnh (Đà Nẵng)");
    }

    @Test
    void createTransaction_missingCoordinates_afterGeocodeNull_returnsBadRequest() {
        Product prod = Product.builder().productId(2).name("P2").stockQty(10).price(new BigDecimal("50")).build();
        when(productRepository.findById(2)).thenReturn(Optional.of(prod));

        // address with city = Da Nang but no coords -> geocode returns null
        Address addr = Address.builder().addressId(2).addressLine("Addr DN").city("Da Nang").latitude(null)
                .longitude(null).build();
        when(addressRepository.findById(2)).thenReturn(Optional.of(addr));
        when(addressRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        when(mapboxService.geocodeAddress(any())).thenReturn(null);

        User current = buildUser(11, 3);
        when(userRepository.findById(11)).thenReturn(Optional.of(current));

        Map<String, Object> body = new HashMap<>();
        body.put("products", List.of(Map.of("product_id", 2, "qty", 1)));
        body.put("address_id", 2);

        ResponseEntity<?> res = svc.createTransaction(body, current);
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        assertThat(((Map<?, ?>) res.getBody()).get("message")).isEqualTo("Thiếu toạ độ giao hàng");
    }

    @Test
    void createTransaction_mapboxDistanceFailure_returns502() {
        Product prod = Product.builder().productId(3).name("P3").stockQty(10).price(new BigDecimal("20")).build();
        when(productRepository.findById(3)).thenReturn(Optional.of(prod));

        Address addr = Address.builder().addressId(3).addressLine("Addr DN").city("Da Nang").latitude(16.06)
                .longitude(108.22).build();
        when(addressRepository.findById(3)).thenReturn(Optional.of(addr));

        User current = buildUser(12, 3);
        when(userRepository.findById(12)).thenReturn(Optional.of(current));

        when(mapboxService.getShopLat()).thenReturn(16.047079);
        when(mapboxService.getShopLng()).thenReturn(108.20623);
        when(mapboxService.getDrivingDistanceMeters(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(-1.0);

        Map<String, Object> body = new HashMap<>();
        body.put("products", List.of(Map.of("product_id", 3, "qty", 1)));
        body.put("address_id", 3);

        ResponseEntity<?> res = svc.createTransaction(body, current);
        assertThat(res.getStatusCode().value()).isEqualTo(502);
        assertThat(((Map<?, ?>) res.getBody()).get("message")).isEqualTo("Không tính được khoảng cách giao hàng");
    }

    @Test
    void createTransaction_under1km_shippingFeeZero_andSaved() {
        Product prod = Product.builder().productId(4).name("P4").stockQty(10).price(new BigDecimal("10")).build();
        when(productRepository.findById(4)).thenReturn(Optional.of(prod));

        Address addr = Address.builder().addressId(4).addressLine("Addr DN").city("Da Nang").latitude(16.05)
                .longitude(108.21).build();
        when(addressRepository.findById(4)).thenReturn(Optional.of(addr));
        when(addressRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User current = buildUser(13, 3);
        when(userRepository.findById(13)).thenReturn(Optional.of(current));

        when(mapboxService.getShopLat()).thenReturn(16.047079);
        when(mapboxService.getShopLng()).thenReturn(108.20623);
        when(mapboxService.getDrivingDistanceMeters(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(500.0);

        when(orderRepository.save(any())).thenAnswer(i -> {
            OrderEntity o = (OrderEntity) i.getArgument(0);
            o.setOrderId(555);
            return o;
        });

        Map<String, Object> body = new HashMap<>();
        body.put("products", List.of(Map.of("product_id", 4, "qty", 1)));
        body.put("address_id", 4);

        ResponseEntity<?> res = svc.createTransaction(body, current);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> data = (Map<?, ?>) ((Map<?, ?>) res.getBody()).get("data");
        assertThat(data.get("id")).isEqualTo(555);

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        OrderEntity savedArg = captor.getValue();
        // subtotal is product price (10) + shipping 0
        assertThat(savedArg.getSubtotalAmount()).isEqualByComparingTo(new BigDecimal("10"));
    }

    @Test
    void createTransaction_between1and3km_shippingFee30000_andSaved() {
        Product prod = Product.builder().productId(5).name("P5").stockQty(10).price(new BigDecimal("15")).build();
        when(productRepository.findById(5)).thenReturn(Optional.of(prod));

        Address addr = Address.builder().addressId(5).addressLine("Addr DN").city("Da Nang").latitude(16.05)
                .longitude(108.21).build();
        when(addressRepository.findById(5)).thenReturn(Optional.of(addr));
        when(addressRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User current = buildUser(14, 3);
        when(userRepository.findById(14)).thenReturn(Optional.of(current));

        when(mapboxService.getShopLat()).thenReturn(16.047079);
        when(mapboxService.getShopLng()).thenReturn(108.20623);
        when(mapboxService.getDrivingDistanceMeters(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(1500.0);

        when(orderRepository.save(any())).thenAnswer(i -> {
            OrderEntity o = (OrderEntity) i.getArgument(0);
            o.setOrderId(666);
            return o;
        });

        Map<String, Object> body = new HashMap<>();
        body.put("products", List.of(Map.of("product_id", 5, "qty", 1)));
        body.put("address_id", 5);

        ResponseEntity<?> res = svc.createTransaction(body, current);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> data = (Map<?, ?>) ((Map<?, ?>) res.getBody()).get("data");
        assertThat(data.get("id")).isEqualTo(666);

        ArgumentCaptor<OrderEntity> captor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository).save(captor.capture());
        OrderEntity savedArg = captor.getValue();
        // subtotal = product price(15) + shipping 30000
        assertThat(savedArg.getSubtotalAmount()).isEqualByComparingTo(new BigDecimal("30015"));
    }
}
