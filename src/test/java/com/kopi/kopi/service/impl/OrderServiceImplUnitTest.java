package com.kopi.kopi.service.impl;

import com.kopi.kopi.controller.GuestOrderController;
import com.kopi.kopi.entity.*;
import com.kopi.kopi.repository.*;
import com.kopi.kopi.service.TableService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderServiceImplUnitTest {

    private OrderRepository orderRepository = Mockito.mock(OrderRepository.class);
    private ProductRepository productRepository = Mockito.mock(ProductRepository.class);
    private AddressRepository addressRepository = Mockito.mock(AddressRepository.class);
    private UserRepository userRepository = Mockito.mock(UserRepository.class);
    private TableService tableService = Mockito.mock(TableService.class);
    private DiningTableRepository diningTableRepository = Mockito.mock(DiningTableRepository.class);
    private UserAddressRepository userAddressRepository = Mockito.mock(UserAddressRepository.class);
    private MapboxService mapboxService = Mockito.mock(MapboxService.class);

    private OrderServiceImpl svc = new OrderServiceImpl(orderRepository, productRepository, addressRepository,
            userRepository, tableService, diningTableRepository, userAddressRepository, mapboxService);

    @Test
    void validateProducts_noProducts_returnsBadRequest() {
        Map<String, Object> empty = new java.util.HashMap<>();
        var res = svc.validateProducts(empty);
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        assertThat(((Map<?, ?>) res.getBody()).get("message")).isEqualTo("No products");
    }

    @Test
    void validateProducts_notFoundAndInsufficient_returnsErrors() {
        // product 1 not found
        when(productRepository.findById(1)).thenReturn(Optional.empty());
        // product 2 found but insufficient
        Product p2 = Product.builder().productId(2).name("P2").stockQty(1).price(new BigDecimal("10.00")).build();
        when(productRepository.findById(2)).thenReturn(Optional.of(p2));

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("products", List.of(
                Map.of("product_id", 1, "qty", 1),
                Map.of("product_id", 2, "qty", 5)));

        ResponseEntity<?> res = svc.validateProducts(body);
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        Map<?, ?> b = (Map<?, ?>) res.getBody();
        assertThat(b.containsKey("errors")).isTrue();
        List<?> errors = (List<?>) b.get("errors");
        assertThat(errors).hasSize(2);
    }

    @Test
    void changeStatus_invalidStatus_returnsBadRequest() {
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("status", "__INVALID__");
        ResponseEntity<?> res = svc.changeStatus(Integer.valueOf(1), payload);
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        assertThat(((Map<?, ?>) res.getBody()).get("message")).isEqualTo("Invalid status");
    }

    @Test
    void changeStatus_completed_insufficientStock_returnsBadRequest() {
        Product prod = Product.builder().productId(10).name("BadProd").stockQty(1).price(new BigDecimal("5.00"))
                .build();
        OrderDetail d = OrderDetail.builder().orderDetailId(1).product(prod).quantity(5).build();
        OrderEntity order = OrderEntity.builder().orderId(9).status("PENDING").orderDetails(List.of(d)).build();
        when(orderRepository.findById(9)).thenReturn(Optional.of(order));

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("status", "COMPLETED");
        ResponseEntity<?> res = svc.changeStatus(Integer.valueOf(9), payload);
        assertThat(res.getStatusCode().value()).isEqualTo(400);
        String msg = (String) ((Map<?, ?>) res.getBody()).get("message");
        assertThat(msg).contains("không đủ");
    }

    @Test
    void changeStatus_completed_success_deductsStock_andPays() {
        Product prod = Product.builder().productId(11).name("GoodProd").stockQty(10).price(new BigDecimal("5.00"))
                .build();
        OrderDetail d = OrderDetail.builder().orderDetailId(2).product(prod).quantity(2).build();
        Payment pay = Payment.builder().paymentId(3).status(null).build();
        OrderEntity order = OrderEntity.builder().orderId(8).status("PENDING").orderDetails(List.of(d))
                .payments(List.of(pay)).build();
        when(orderRepository.findById(8)).thenReturn(Optional.of(order));
        when(productRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(orderRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("status", "COMPLETED");
        ResponseEntity<?> res = svc.changeStatus(Integer.valueOf(8), payload);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        // product stock should be reduced by 2
        assertThat(prod.getStockQty()).isEqualTo(8);
        // payment status should be PAID
        assertThat(order.getPayments().get(0).getStatus()).isEqualTo(com.kopi.kopi.entity.enums.PaymentStatus.PAID);
        verify(productRepository, times(1)).save(prod);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void createGuestTableOrder_tableNotFound_returns404() {
        GuestOrderController.GuestOrderRequest req = new GuestOrderController.GuestOrderRequest(null, 9999, List.of(),
                "", 1, false);
        when(diningTableRepository.findByNumber(9999)).thenReturn(Optional.empty());
        ResponseEntity<?> res = svc.createGuestTableOrder(req);
        assertThat(res.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void createGuestTableOrder_success_savesOrder_and_occupiesTable() {
        DiningTable table = DiningTable.builder().tableId(5).number(12).status("AVAILABLE").build();
        when(diningTableRepository.findByNumber(12)).thenReturn(Optional.of(table));

        Product prod = Product.builder().productId(21).name("GProd").stockQty(10).price(new BigDecimal("10")).build();
        when(productRepository.findById(21)).thenReturn(Optional.of(prod));

        // Save order returns with id
        when(orderRepository.save(any())).thenAnswer(i -> {
            OrderEntity o = (OrderEntity) i.getArgument(0);
            o.setOrderId(123);
            return o;
        });

        GuestOrderController.GuestOrderItem item = new GuestOrderController.GuestOrderItem(21, 2);
        GuestOrderController.GuestOrderRequest req = new GuestOrderController.GuestOrderRequest(null, 12, List.of(item),
                "notes", 1, false);

        ResponseEntity<?> res = svc.createGuestTableOrder(req);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        Map<?, ?> data = (Map<?, ?>) body.get("data");
        assertThat(data.get("id")).isEqualTo(123);
        verify(tableService, times(1)).setOccupiedIfHasPendingOrders(5);
    }
}
