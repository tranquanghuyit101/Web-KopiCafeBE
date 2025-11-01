package com.kopi.kopi.service.impl;

import com.kopi.kopi.controller.GuestOrderController;
import com.kopi.kopi.entity.*;
import com.kopi.kopi.entity.enums.PaymentMethod;
import com.kopi.kopi.entity.enums.PaymentStatus;
import com.kopi.kopi.repository.*;
import com.kopi.kopi.service.TableService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

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
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        // nothing extra
    }

    // validateProducts: empty -> bad request
    @Test
    void validateProducts_NoProducts_BadRequest() {
        Map<String, Object> body = new HashMap<>();
        ResponseEntity<?> resp = service.validateProducts(body);
        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        assertThat(((Map<?, ?>) resp.getBody()).get("message")).isEqualTo("No products");
    }

    // validateProducts: product not found and insufficient stock
    @Test
    void validateProducts_InsufficientAndNotFound() {
        Map<String, Object> p1 = Map.of("product_id", 10, "qty", 2);
        Map<String, Object> p2 = Map.of("product_id", 11, "qty", 1);
        Map<String, Object> body = new HashMap<>();
        body.put("products", List.of(p1, p2));

        // product 10 exists but low stock
        Product prod10 = new Product();
        prod10.setProductId(10);
        prod10.setName("P10");
        prod10.setStockQty(1);
        when(productRepository.findById(10)).thenReturn(Optional.of(prod10));
        // product 11 missing
        when(productRepository.findById(11)).thenReturn(Optional.empty());

        ResponseEntity<?> resp = service.validateProducts(body);
        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        Map<?, ?> bodyResp = (Map<?, ?>) resp.getBody();
        assertThat(bodyResp.get("message")).isEqualTo("Insufficient stock");
        var errors = (List<?>) bodyResp.get("errors");
        assertThat(errors).hasSize(2);
    }

    // changeStatus: invalid status -> bad request
    @Test
    void changeStatus_InvalidStatus_BadRequest() {
        Map<String, Object> payload = Map.of("status", "UNKNOWN");
        ResponseEntity<?> resp = service.changeStatus(1, payload);
        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        assertThat(((Map<?, ?>) resp.getBody()).get("message")).isEqualTo("Invalid status");
    }

    // changeStatus: COMPLETED but product has insufficient stock
    @Test
    void changeStatus_Completed_InsufficientStock() {
        OrderEntity order = new OrderEntity();
        order.setOrderId(5);
        order.setStatus("PENDING");
        Product p = new Product();
        p.setProductId(1);
        p.setName("Milk");
        p.setStockQty(1);
        OrderDetail d = OrderDetail.builder().orderDetailId(7).product(p).quantity(2).build();
        order.setOrderDetails(List.of(d));
        when(orderRepository.findById(5)).thenReturn(Optional.of(order));

        Map<String, Object> payload = Map.of("status", "COMPLETED");
        ResponseEntity<?> resp = service.changeStatus(5, payload);
        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        String msg = (String) ((Map<?, ?>) resp.getBody()).get("message");
        assertThat(msg).contains("không đủ số lượng");
    }

    // changeStatus: COMPLETED success -> product stock deducted, payment updated,
    // table service called
    @Test
    void changeStatus_Completed_Success() {
        OrderEntity order = new OrderEntity();
        order.setOrderId(6);
        order.setStatus("PENDING");
        Product p = new Product();
        p.setProductId(2);
        p.setName("Bread");
        p.setStockQty(5);
        OrderDetail d = OrderDetail.builder().orderDetailId(8).product(p).quantity(2).build();
        order.setOrderDetails(List.of(d));
        Payment pay = Payment.builder().status(PaymentStatus.PENDING).build();
        order.setPayments(new ArrayList<>(List.of(pay)));
        DiningTable tbl = new DiningTable();
        tbl.setTableId(99);
        order.setTable(tbl);

        when(orderRepository.findById(6)).thenReturn(Optional.of(order));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> payload = Map.of("status", "COMPLETED");
        ResponseEntity<?> resp = service.changeStatus(6, payload);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        // product stock reduced
        assertThat(p.getStockQty()).isEqualTo(3);
        // payment status updated to PAID
        assertThat(order.getPayments().get(0).getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(productRepository).save(p);
        verify(orderRepository).save(order);
        verify(tableService).setAvailableIfNoPendingOrders(eq(99));
    }

    // createGuestTableOrder: table not found -> 404
    @Test
    void createGuestTableOrder_TableNotFound() {
        GuestOrderController.GuestOrderRequest req = mock(GuestOrderController.GuestOrderRequest.class);
        when(req.qr_token()).thenReturn("X");
        when(diningTableRepository.findByQrToken("X")).thenReturn(Optional.empty());
        when(req.table_number()).thenReturn(null);

        ResponseEntity<?> resp = service.createGuestTableOrder(req);
        assertThat(resp.getStatusCodeValue()).isEqualTo(404);
        assertThat(((Map<?, ?>) resp.getBody()).get("message")).isEqualTo("Table not found");
    }

    // createGuestTableOrder: table disabled
    @Test
    void createGuestTableOrder_TableDisabled() {
        GuestOrderController.GuestOrderRequest req = mock(GuestOrderController.GuestOrderRequest.class);
        when(req.qr_token()).thenReturn(null);
        when(req.table_number()).thenReturn(12);
        DiningTable t = new DiningTable();
        t.setTableId(12);
        t.setNumber(12);
        t.setStatus("DISABLED");
        when(diningTableRepository.findByNumber(12)).thenReturn(Optional.of(t));

        ResponseEntity<?> resp = service.createGuestTableOrder(req);
        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        assertThat(((Map<?, ?>) resp.getBody()).get("message")).isEqualTo("Table disabled");
    }

    // createGuestTableOrder: success flow
    @Test
    void createGuestTableOrder_Success() {
        GuestOrderController.GuestOrderRequest req = mock(GuestOrderController.GuestOrderRequest.class);
        when(req.qr_token()).thenReturn(null);
        when(req.table_number()).thenReturn(42);
        DiningTable t = new DiningTable();
        t.setTableId(42);
        t.setNumber(42);
        t.setStatus("ACTIVE");
        when(diningTableRepository.findByNumber(42)).thenReturn(Optional.of(t));

        GuestOrderController.GuestOrderItem it = mock(GuestOrderController.GuestOrderItem.class);
        when(it.product_id()).thenReturn(100);
        when(it.qty()).thenReturn(1);
        when(req.products()).thenReturn(List.of(it));
        Product prod = new Product();
        prod.setProductId(100);
        prod.setName("Latte");
        prod.setPrice(new BigDecimal("20000"));
        prod.setStockQty(10);
        when(productRepository.findById(100)).thenReturn(Optional.of(prod));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            OrderEntity o = inv.getArgument(0);
            o.setOrderId(777);
            return o;
        });

        when(req.paid()).thenReturn(false);
        when(req.payment_id()).thenReturn(1);
        when(req.notes()).thenReturn("note");

        ResponseEntity<?> resp = service.createGuestTableOrder(req);
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        Map<?, ?> data = (Map<?, ?>) ((Map<?, ?>) resp.getBody()).get("data");
        assertThat(data.get("id")).isEqualTo(777);
        assertThat(data.get("table_number")).isEqualTo(42);
        verify(tableService).setOccupiedIfHasPendingOrders(42);
    }
}