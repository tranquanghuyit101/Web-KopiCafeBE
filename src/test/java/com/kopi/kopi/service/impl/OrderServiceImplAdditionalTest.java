package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.*;
import com.kopi.kopi.repository.*;
import com.kopi.kopi.service.TableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplAdditionalTest {

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

    @Test
    void getUserTransactions_returnsMappedItems_andMeta() {
        User u = User.builder().userId(77).fullName("Alice").build();
        Product p = Product.builder().productId(101).name("Capp").imgUrl("img.jpg").price(new BigDecimal("20000"))
                .build();
        OrderDetail d = OrderDetail.builder().orderDetailId(9).product(p).productNameSnapshot(null).quantity(2)
                .unitPrice(new BigDecimal("20000")).lineTotal(new BigDecimal("40000")).build();
        OrderEntity o = OrderEntity.builder().orderId(300).status("PENDING").createdAt(LocalDateTime.now())
                .orderDetails(List.of(d)).customer(u).build();

        when(orderRepository.findByCustomer_UserId(org.mockito.ArgumentMatchers.eq(77),
                org.mockito.ArgumentMatchers.any())).thenReturn(new PageImpl<>(List.of(o)));

        var resp = service.getUserTransactions(77, 1, 10);
        assertThat(resp).containsKeys("data", "meta");
        var data = (List<?>) resp.get("data");
        assertThat(data).hasSize(1);
        var first = (Map<?, ?>) data.get(0);
        assertThat(first.get("id")).isEqualTo(300);
        assertThat(first.get("products")).isInstanceOf(List.class);
    }

    @Test
    void getTransactionDetail_forbiddenWhenDifferentUser_returns403() {
        User owner = User.builder().userId(5).fullName("Owner").build();
        OrderEntity o = OrderEntity.builder().orderId(500).customer(owner).build();
        when(orderRepository.findById(500)).thenReturn(Optional.of(o));

        ResponseEntity<?> res = service.getTransactionDetail(500, 99);
        assertThat(res.getStatusCode().value()).isEqualTo(403);
        assertThat(((Map<?, ?>) res.getBody()).get("message")).isEqualTo("Forbidden");
    }

    @Test
    void getTransactionDetail_ok_returnsDetailWithProducts() {
        User owner = User.builder().userId(6).fullName("Bob").build();
        Product p = Product.builder().productId(202).name("Mocha").imgUrl("mocha.jpg").price(new BigDecimal("30000"))
                .build();
        OrderDetail d = OrderDetail.builder().orderDetailId(11).product(p).productNameSnapshot(null).quantity(1)
                .unitPrice(new BigDecimal("30000")).lineTotal(new BigDecimal("30000")).build();
        OrderEntity o = OrderEntity.builder().orderId(501).customer(owner).orderDetails(List.of(d))
                .totalAmount(new BigDecimal("30000")).createdAt(LocalDateTime.now()).build();
        when(orderRepository.findById(501)).thenReturn(Optional.of(o));

        ResponseEntity<?> res = service.getTransactionDetail(501, 6);
        assertThat(res.getStatusCode().value()).isEqualTo(200);
        Map<?, ?> body = (Map<?, ?>) res.getBody();
        assertThat(body.containsKey("data")).isTrue();
        var arr = (List<?>) body.get("data");
        assertThat(arr).hasSize(1);
        var det = (Map<?, ?>) arr.get(0);
        assertThat(det.get("id")).isEqualTo(501);
        assertThat(det.get("products")).isInstanceOf(List.class);
    }

    @Test
    void listPending_tableType_callsTableRepositoryAndReturnsMeta() {
        OrderEntity o1 = OrderEntity.builder().orderId(700).status("PENDING").address(null)
                .createdAt(LocalDateTime.now()).build();
        when(orderRepository.findByStatusNotInAndAddressIsNull(any(), any())).thenReturn(new PageImpl<>(List.of(o1)));

        var resp = service.listPending("PENDING", "TABLE", 1, 10);
        assertThat(resp).containsKeys("data", "meta");
        var data = (List<?>) resp.get("data");
        assertThat(data).hasSize(1);
    }
}
