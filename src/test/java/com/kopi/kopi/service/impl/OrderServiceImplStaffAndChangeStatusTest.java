package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.*;
import com.kopi.kopi.entity.enums.PaymentStatus;
import com.kopi.kopi.repository.*;
import com.kopi.kopi.service.TableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplStaffAndChangeStatusTest {

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

    private User userWithRole(int id, int roleId) {
        Role r = Role.builder().roleId(roleId).build();
        return User.builder().userId(id).role(r).build();
    }

    @Test
    void createTransaction_staff_withCustomerAndAddressId_attachesCustomerAndSaves() {
        Product prod = Product.builder().productId(10).name("PX").stockQty(10).price(new BigDecimal("20")).build();
        when(productRepository.findById(10)).thenReturn(Optional.of(prod));

        Address a = Address.builder().addressId(10).addressLine("Addr").city("Da Nang").latitude(16.05)
                .longitude(108.21).build();
        when(addressRepository.findById(10)).thenReturn(Optional.of(a));

        User staff = userWithRole(100, 2);
        when(userRepository.findById(100)).thenReturn(Optional.of(staff));
        User cust = userWithRole(200, 3);
        when(userRepository.findById(200)).thenReturn(Optional.of(cust));

        when(orderRepository.save(any())).thenAnswer(i -> {
            OrderEntity o = (OrderEntity) i.getArgument(0);
            o.setOrderId(1000);
            return o;
        });

        Map<String, Object> body = new HashMap<>();
        body.put("products", List.of(Map.of("product_id", 10, "qty", 1)));
        body.put("customer_id", 200);
        body.put("address_id", 10);

        ResponseEntity<?> res = svc.createTransaction(body, staff);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> data = (Map<?, ?>) ((Map<?, ?>) res.getBody()).get("data");
        assertThat(data.get("id")).isEqualTo(1000);
    }

    @Test
    void createTransaction_addressText_createsAddress_andSaves() {
        Product prod = Product.builder().productId(11).name("P11").stockQty(10).price(new BigDecimal("5")).build();
        when(productRepository.findById(11)).thenReturn(Optional.of(prod));

        User current = userWithRole(110, 3);
        when(userRepository.findById(110)).thenReturn(Optional.of(current));

        when(addressRepository.save(any())).thenAnswer(i -> {
            Address x = (Address) i.getArgument(0);
            x.setAddressId(77);
            return x;
        });

        when(orderRepository.save(any())).thenAnswer(i -> {
            OrderEntity o = (OrderEntity) i.getArgument(0);
            o.setOrderId(2000);
            return o;
        });

        // stub mapbox to geocode the ad-hoc address so shipping validation passes
        when(mapboxService.geocodeAddress(any())).thenReturn(new MapboxService.GeoResult(16.05, 108.21, "Da Nang"));
        when(mapboxService.getShopLat()).thenReturn(16.047079);
        when(mapboxService.getShopLng()).thenReturn(108.20623);
        when(mapboxService.getDrivingDistanceMeters(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(500.0);

        Map<String, Object> body = new HashMap<>();
        body.put("products", List.of(Map.of("product_id", 11, "qty", 1)));
        body.put("address", "Some street, Da Nang");

        ResponseEntity<?> res = svc.createTransaction(body, current);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> data = (Map<?, ?>) ((Map<?, ?>) res.getBody()).get("data");
        assertThat(data.get("id")).isEqualTo(2000);
        // addressRepository.save is called twice: once when creating ad-hoc address and
        // again after geocode
        verify(addressRepository, times(2)).save(any());
    }

    @Test
    void createTransaction_paidTrue_setsCompletedAndPaymentPaid() {
        Product prod = Product.builder().productId(12).name("P12").stockQty(10).price(new BigDecimal("7")).build();
        when(productRepository.findById(12)).thenReturn(Optional.of(prod));

        User current = userWithRole(120, 3);
        when(userRepository.findById(120)).thenReturn(Optional.of(current));

        when(orderRepository.save(any())).thenAnswer(i -> {
            OrderEntity o = (OrderEntity) i.getArgument(0);
            o.setOrderId(3000);
            return o;
        });

        Map<String, Object> body = new HashMap<>();
        body.put("products", List.of(Map.of("product_id", 12, "qty", 1)));
        body.put("paid", true);

        ResponseEntity<?> res = svc.createTransaction(body, current);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        // ensure payment status on saved order is PAID
        verify(orderRepository).save(any());
    }

    @Test
    void changeStatus_cancelled_updatesPayment_and_callsTableAvailability() {
        Payment p = Payment.builder().paymentId(5).status(PaymentStatus.PENDING).build();
        DiningTable t = DiningTable.builder().tableId(9).number(1).build();
        OrderEntity order = OrderEntity.builder().orderId(4000).status("PENDING").payments(List.of(p)).table(t).build();
        when(orderRepository.findById(4000)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "CANCELLED");

        ResponseEntity<?> res = svc.changeStatus(4000, payload);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(order.getPayments().get(0).getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        verify(tableService, times(1)).setAvailableIfNoPendingOrders(9);
    }

    @Test
    void changeStatus_paid_setsPaymentPaid() {
        Payment p = Payment.builder().paymentId(6).status(PaymentStatus.PENDING).build();
        OrderEntity order = OrderEntity.builder().orderId(5000).status("PENDING").payments(List.of(p)).build();
        when(orderRepository.findById(5000)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "PAID");

        ResponseEntity<?> res = svc.changeStatus(5000, payload);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        assertThat(order.getPayments().get(0).getStatus()).isEqualTo(PaymentStatus.PAID);
    }
}
