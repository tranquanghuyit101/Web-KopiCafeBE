package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.*;
import com.kopi.kopi.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Nested
    @DisplayName("Feature: List Orders by Status")
    class ListPendingOrdersTests {

        @Test
        @DisplayName("should Return First Page When Requesting Pending Orders")
        void should_ReturnFirstPage_When_RequestingPendingOrders() {
            // === Given ===
            Address mockAddress = new Address();
            mockAddress.setAddressLine("456 Admin Avenue");

            OrderEntity order1 = new OrderEntity();
            order1.setOrderId(10);
            order1.setStatus("PENDING");
            order1.setAddress(mockAddress);
            order1.setCreatedAt(LocalDateTime.now());
            order1.setTotalAmount(new BigDecimal("100.00"));

            OrderEntity order2 = new OrderEntity();
            order2.setOrderId(11);
            order2.setStatus("PENDING");
            order2.setTotalAmount(new BigDecimal("50.25"));

            List<OrderEntity> pageContent = List.of(order1, order2);
            Pageable requestedPageable = PageRequest.of(0, 5);
            long totalElements = 12;

            Page<OrderEntity> orderPage = new PageImpl<>(pageContent, requestedPageable, totalElements);
            when(orderRepository.findByStatus(eq("PENDING"), any(Pageable.class))).thenReturn(orderPage);

            // === When ===
            Map<String, Object> result = orderService.listPending("PENDING", null, 1, 5);

            // === Then ===
            verify(orderRepository).findByStatus("PENDING", PageRequest.of(0, 5));

            assertThat(result).isNotNull();
            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
            assertThat(data).hasSize(2);
            assertThat(data.get(0).get("id")).isEqualTo(10);
            assertThat(data.get(0).get("status")).isEqualTo("PENDING");
            assertThat(data.get(0).get("address")).isEqualTo("456 Admin Avenue");
            assertThat(data.get(1).get("address")).isNull();

            Map<String, Object> meta = (Map<String, Object>) result.get("meta");
            assertThat(meta.get("currentPage")).isEqualTo(1);
            assertThat(meta.get("totalPage")).isEqualTo(3);
            assertThat(meta.get("prev")).isEqualTo(false);
            assertThat(meta.get("next")).isEqualTo(true);
        }

        @Test
        @DisplayName("should Return Last Page With Correct Meta When Requesting Final Page")
        void should_ReturnLastPageWithCorrectMeta_When_RequestingFinalPage() {
            // === Given ===
            OrderEntity order1 = new OrderEntity();
            order1.setOrderId(20);
            order1.setStatus("COMPLETED");

            List<OrderEntity> lastPageContent = List.of(order1);
            Pageable requestedPageable = PageRequest.of(2, 10);
            long totalElements = 21;

            Page<OrderEntity> orderPage = new PageImpl<>(lastPageContent, requestedPageable, totalElements);
            when(orderRepository.findByStatus(eq("COMPLETED"), any(Pageable.class))).thenReturn(orderPage);

            // === When ===
            Map<String, Object> result = orderService.listPending("COMPLETED", null, 3, 10);

            // === Then ===
            verify(orderRepository).findByStatus("COMPLETED", PageRequest.of(2, 10));

            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
            assertThat(data).hasSize(1);

            Map<String, Object> meta = (Map<String, Object>) result.get("meta");
            assertThat(meta.get("currentPage")).isEqualTo(3);
            assertThat(meta.get("totalPage")).isEqualTo(3);
            assertThat(meta.get("prev")).isEqualTo(true);
            assertThat(meta.get("next")).isEqualTo(false);
        }

        @Test
        @DisplayName("should Return Order With Formatted Products When Order Has Multiple Details")
        void should_ReturnOrderWithFormattedProducts_When_OrderHasMultipleDetails() {
            // === Given ===
            OrderDetail detail1 = new OrderDetail();
            detail1.setProductNameSnapshot("Americano");
            detail1.setQuantity(2);
            detail1.setLineTotal(new BigDecimal("5.00"));

            OrderDetail detail2 = new OrderDetail();
            detail2.setProductNameSnapshot("Croissant");
            detail2.setQuantity(1);
            detail2.setLineTotal(new BigDecimal("2.50"));

            OrderEntity mockOrder = new OrderEntity();
            mockOrder.setOrderId(30);
            mockOrder.setStatus("PENDING");
            mockOrder.setOrderDetails(List.of(detail1, detail2));

            Page<OrderEntity> orderPage = new PageImpl<>(List.of(mockOrder));
            when(orderRepository.findByStatus(eq("PENDING"), any(Pageable.class))).thenReturn(orderPage);

            // === When ===
            Map<String, Object> result = orderService.listPending("PENDING", null, 1, 5);

            // === Then ===
            verify(orderRepository).findByStatus("PENDING", PageRequest.of(0, 5));

            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
            assertThat(data).hasSize(1);

            Map<String, Object> orderMap = data.get(0);
            List<Map<String, Object>> products = (List<Map<String, Object>>) orderMap.get("products");
            assertThat(products).hasSize(2);

            Map<String, Object> product1Map = products.get(0);
            assertThat(product1Map.get("product_name")).isEqualTo("Americano");
            assertThat(product1Map.get("qty")).isEqualTo(2);
            assertThat(product1Map.get("subtotal")).isEqualTo(new BigDecimal("5.00"));
        }
    }

    @Nested
    @DisplayName("Feature: Get User Transactions")
    class GetUserTransactionsTests {

        private User testUser;

        @BeforeEach
        void setUp() {
            testUser = new User();
            testUser.setUserId(101);
            testUser.setFullName("Kopi Customer");
        }

        @Test
        @DisplayName("should Return Formatted Transaction When User Has One Order")
        void should_ReturnFormattedTransaction_When_UserHasOneOrder() {
            // === Given ===
            Product mockProduct = new Product();
            mockProduct.setName("Latte");
            mockProduct.setImgUrl("http://example.com/latte.jpg");

            OrderDetail mockDetail = new OrderDetail();
            mockDetail.setProduct(mockProduct);
            mockDetail.setQuantity(2);
            mockDetail.setLineTotal(new BigDecimal("7.00"));

            OrderEntity mockOrder = new OrderEntity();
            mockOrder.setOrderId(1);
            mockOrder.setStatus("COMPLETED");
            mockOrder.setTotalAmount(new BigDecimal("7.50"));
            mockOrder.setOrderDetails(List.of(mockDetail));

            Page<OrderEntity> orderPage = new PageImpl<>(List.of(mockOrder));
            when(orderRepository.findByCustomer_UserId(eq(testUser.getUserId()), any(Pageable.class)))
                    .thenReturn(orderPage);

            // === When ===
            Map<String, Object> result = orderService.getUserTransactions(testUser.getUserId(), 1, 10);

            // === Then ===
            verify(orderRepository).findByCustomer_UserId(eq(101), eq(PageRequest.of(0, 10)));

            assertThat(result).isNotNull();
            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
            assertThat(data).hasSize(1);

            Map<String, Object> orderMap = data.get(0);
            assertThat(orderMap.get("id")).isEqualTo(1);
            assertThat(orderMap.get("grand_total")).isEqualTo(new BigDecimal("7.50"));
            assertThat(orderMap.get("status_name")).isEqualTo("COMPLETED");

            List<Map<String, Object>> products = (List<Map<String, Object>>) orderMap.get("products");
            assertThat(products).hasSize(1);
            Map<String, Object> productMap = products.get(0);
            assertThat(productMap.get("product_name")).isEqualTo("Latte");
            assertThat(productMap.get("qty")).isEqualTo(2);
            assertThat(productMap.get("subtotal")).isEqualTo(new BigDecimal("7.00"));

            Map<String, Object> meta = (Map<String, Object>) result.get("meta");
            assertThat(meta.get("currentPage")).isEqualTo(1);
            assertThat(meta.get("totalPage")).isEqualTo(1);
            assertThat(meta.get("prev")).isEqualTo(false);
            assertThat(meta.get("next")).isEqualTo(false);
        }

        @Test
        @DisplayName("should Return Grand Total As Zero When Order Total Amount Is Null")
        void should_ReturnGrandTotalAsZero_When_OrderTotalAmountIsNull() {
            // === Given ===
            OrderEntity mockOrder = new OrderEntity();
            mockOrder.setOrderId(2);
            mockOrder.setStatus("PENDING");
            mockOrder.setTotalAmount(null);
            mockOrder.setOrderDetails(new ArrayList<>());

            Page<OrderEntity> orderPage = new PageImpl<>(List.of(mockOrder));
            when(orderRepository.findByCustomer_UserId(any(Integer.class), any(Pageable.class)))
                    .thenReturn(orderPage);

            // === When ===
            Map<String, Object> result = orderService.getUserTransactions(testUser.getUserId(), 1, 10);

            // === Then ===
            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
            assertThat(data).hasSize(1);
            Map<String, Object> orderMap = data.get(0);

            assertThat(orderMap.get("grand_total")).isEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("should Throw NullPointerException When Page Is Null")
        void should_ThrowNullPointerException_When_PageIsNull() {
            // === When & Then ===
            assertThrows(NullPointerException.class, () -> {
                orderService.getUserTransactions(testUser.getUserId(), null, 10);
            });
        }

        @Test
        @DisplayName("should Return Correct Status Name When Order Status Is Cancelled")
        void should_ReturnCorrectStatusName_When_OrderStatusIsCancelled() {
            // === Given ===
            OrderEntity mockOrder = new OrderEntity();
            mockOrder.setOrderId(3);
            mockOrder.setStatus("CANCELLED");
            mockOrder.setTotalAmount(new BigDecimal("25.00"));
            mockOrder.setOrderDetails(new ArrayList<>());

            Page<OrderEntity> orderPage = new PageImpl<>(List.of(mockOrder));
            when(orderRepository.findByCustomer_UserId(any(Integer.class), any(Pageable.class)))
                    .thenReturn(orderPage);

            // === When ===
            Map<String, Object> result = orderService.getUserTransactions(testUser.getUserId(), 1, 10);

            // === Then ===
            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
            assertThat(data).hasSize(1);
            Map<String, Object> orderMap = data.get(0);

            assertThat(orderMap.get("status_name")).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("should Handle Pagination Correctly When Data Set Is Large")
        void should_HandlePaginationCorrectly_When_DataSetIsLarge() {
            // === Given ===
            List<OrderEntity> allOrders = IntStream.rangeClosed(1, 50)
                    .mapToObj(i -> {
                        OrderEntity order = new OrderEntity();
                        order.setOrderId(i);
                        order.setStatus("COMPLETED");
                        order.setTotalAmount(BigDecimal.TEN);
                        order.setOrderDetails(new ArrayList<>());
                        return order;
                    })
                    .collect(Collectors.toList());

            int page = 3;
            int limit = 10;
            Pageable requestedPageable = PageRequest.of(page - 1, limit);

            int start = (int) requestedPageable.getOffset();
            int end = Math.min((start + requestedPageable.getPageSize()), allOrders.size());
            List<OrderEntity> pageContent = allOrders.subList(start, end);

            Page<OrderEntity> orderPage = new PageImpl<>(pageContent, requestedPageable, allOrders.size());
            when(orderRepository.findByCustomer_UserId(eq(testUser.getUserId()), any(Pageable.class)))
                    .thenReturn(orderPage);

            // === When ===
            Map<String, Object> result = orderService.getUserTransactions(testUser.getUserId(), page, limit);

            // === Then ===
            verify(orderRepository).findByCustomer_UserId(eq(101), eq(PageRequest.of(2, 10)));

            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
            assertThat(data).hasSize(10);

            Map<String, Object> meta = (Map<String, Object>) result.get("meta");
            assertThat(meta.get("currentPage")).isEqualTo(3);
            assertThat(meta.get("totalPage")).isEqualTo(5);
            assertThat(meta.get("prev")).isEqualTo(true);
            assertThat(meta.get("next")).isEqualTo(true);
        }
    }
}