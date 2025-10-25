package com.kopi.kopi.impl;

import com.kopi.kopi.entity.OrderEntity;
import com.kopi.kopi.entity.OrderDetail;
import com.kopi.kopi.entity.Product;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import com.kopi.kopi.entity.*;
import com.kopi.kopi.entity.enums.PaymentMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import java.util.*;
import com.kopi.kopi.entity.Address;




@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService - Get User Transactions Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Initialize a common user object for all tests
        testUser = new User();
        testUser.setUserId(101);
        testUser.setFullName("Kopi Customer");
    }

    // --- Test Cases ---

    @Test
    @DisplayName("should_ReturnFormattedTransaction_When_UserHasOneOrder")
    void should_ReturnFormattedTransaction_When_UserHasOneOrder() {
        // === Given ===
        // 1. Setup mock entities
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

        // 2. Setup mock repository response
        Page<OrderEntity> orderPage = new PageImpl<>(List.of(mockOrder));
        when(orderRepository.findByCustomer_UserId(eq(testUser.getUserId()), any(Pageable.class)))
                .thenReturn(orderPage);

        // === When ===
    Map<String, Object> result = orderService.getUserTransactions(testUser.getUserId(), 1, 10);

        // === Then ===
        // 1. Verify repository interaction
        verify(orderRepository).findByCustomer_UserId(eq(101), eq(PageRequest.of(0, 10)));

        // 2. Assert the structure and content of the response
        assertThat(result).isNotNull();
        assertThat(result.get("data")).isInstanceOf(List.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
        assertThat(data).hasSize(1);

        // 3. Assert order details
        Map<String, Object> orderMap = data.get(0);
        assertThat(orderMap.get("id")).isEqualTo(1);
        assertThat(orderMap.get("grand_total")).isEqualTo(new BigDecimal("7.50"));
        assertThat(orderMap.get("status_name")).isEqualTo("COMPLETED");

        // 4. Assert product details within the order
        List<Map<String, Object>> products = (List<Map<String, Object>>) orderMap.get("products");
        assertThat(products).hasSize(1);
        Map<String, Object> productMap = products.get(0);
        assertThat(productMap.get("product_name")).isEqualTo("Latte");
        assertThat(productMap.get("qty")).isEqualTo(2);
        assertThat(productMap.get("subtotal")).isEqualTo(new BigDecimal("7.00"));

        // 5. Assert meta pagination details
        Map<String, Object> meta = (Map<String, Object>) result.get("meta");
        assertThat(meta.get("currentPage")).isEqualTo(1);
        assertThat(meta.get("totalPage")).isEqualTo(1);
        assertThat(meta.get("prev")).isEqualTo(false);
        assertThat(meta.get("next")).isEqualTo(false);
    }

    @Test
    @DisplayName("should_ReturnGrandTotalAsZero_When_OrderTotalAmountIsNull")
    void should_ReturnGrandTotalAsZero_When_OrderTotalAmountIsNull() {
        // === Given ===
        OrderEntity mockOrder = new OrderEntity();
        mockOrder.setOrderId(2);
        mockOrder.setStatus("PENDING");
        mockOrder.setTotalAmount(null); // The critical condition for this test
        mockOrder.setOrderDetails(new ArrayList<>()); // Empty details for simplicity

        Page<OrderEntity> orderPage = new PageImpl<>(List.of(mockOrder));
        when(orderRepository.findByCustomer_UserId(any(Integer.class), any(Pageable.class)))
                .thenReturn(orderPage);

        // === When ===
    Map<String, Object> result = orderService.getUserTransactions(testUser.getUserId(), 1, 10);

        // === Then ===
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
        assertThat(data).hasSize(1);
        Map<String, Object> orderMap = data.get(0);

        // Assert that the private helper 'defaultBigDecimal' correctly converted null to zero
        assertThat(orderMap.get("grand_total")).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("should_ThrowNullPointerException_When_PageIsNull")
    void should_ThrowNullPointerException_When_PageIsNull() {
        // === Given ===
        // No repository mocking is needed as the code fails before the call.

        // === When & Then ===
        // The method attempts `page - 1` on a null Integer, causing an NPE.
        assertThrows(NullPointerException.class, () -> {
            orderService.getUserTransactions(testUser.getUserId(), null, 10);
        });
    }

    @Test
    @DisplayName("should_ReturnCorrectStatusName_When_OrderStatusIsCancelled")
    void should_ReturnCorrectStatusName_When_OrderStatusIsCancelled() {
        // === Given ===
        OrderEntity mockOrder = new OrderEntity();
        mockOrder.setOrderId(3);
        mockOrder.setStatus("CANCELLED"); // The critical condition for this test
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
    @DisplayName("should_HandlePaginationCorrectly_When_DataSetIsLarge")
    void should_HandlePaginationCorrectly_When_DataSetIsLarge() {
        // === Given ===
        // 1. Simulate a large dataset of 50 orders
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

        // 2. Request page 3 with a limit of 10
        int page = 3;
        int limit = 10;
        Pageable requestedPageable = PageRequest.of(page - 1, limit);

        // 3. Define the slice of data that Spring Data JPA would return
        int start = (int) requestedPageable.getOffset();
        int end = Math.min((start + requestedPageable.getPageSize()), allOrders.size());
        List<OrderEntity> pageContent = allOrders.subList(start, end);

        // 4. Create the mock Page object with the slice and total count
        Page<OrderEntity> orderPage = new PageImpl<>(pageContent, requestedPageable, allOrders.size());
        when(orderRepository.findByCustomer_UserId(eq(testUser.getUserId()), any(Pageable.class)))
                .thenReturn(orderPage);

        // === When ===
            Map<String, Object> result = orderService.getUserTransactions(testUser.getUserId(), page, limit);

        // === Then ===
        // 1. Verify repository interaction with the correct pageable
        verify(orderRepository).findByCustomer_UserId(eq(101), eq(PageRequest.of(2, 10)));

        // 2. Assert the size of the returned data slice
        List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
        assertThat(data).hasSize(10); // Should be 10 items on page 3

        // 3. Assert the pagination metadata
        Map<String, Object> meta = (Map<String, Object>) result.get("meta");
        assertThat(meta.get("currentPage")).isEqualTo(3);
        assertThat(meta.get("totalPage")).isEqualTo(5); // 50 items / 10 per page = 5 pages
        assertThat(meta.get("prev")).isEqualTo(true); // We are on page 3, so there is a previous page
        assertThat(meta.get("next")).isEqualTo(true); // We are on page 3 of 5, so there is a next page
    }




        // --- Test Cases ---

        @Test
        @DisplayName("should_ReturnFullOrderDetail_When_OrderExistsAndBelongsToUser")
        void should_ReturnFullOrderDetail_When_OrderExistsAndBelongsToUser() {
            // === Given ===
            // 1. Create all necessary mock entities for a complete order
            Address mockAddress = new Address();
            mockAddress.setAddressLine("123 Kopi Lane");

            Payment mockPayment = new Payment();
            mockPayment.setMethod(PaymentMethod.CASH);

            Product mockProduct = new Product();
            mockProduct.setName("Espresso");
            mockProduct.setImgUrl("http://images.com/espresso.png");

            OrderDetail mockDetail = new OrderDetail();
            mockDetail.setOrderDetailId(2001);
            mockDetail.setProduct(mockProduct);
            mockDetail.setQuantity(1);
            mockDetail.setLineTotal(new BigDecimal("3.50"));

            OrderEntity mockOrder = new OrderEntity();
            mockOrder.setOrderId(1);
            mockOrder.setCustomer(testUser); // Crucially, the order belongs to our testUser
            mockOrder.setAddress(mockAddress);
            mockOrder.setPayments(List.of(mockPayment));
            mockOrder.setOrderDetails(List.of(mockDetail));
            mockOrder.setStatus("COMPLETED");
            mockOrder.setTotalAmount(new BigDecimal("4.00"));
            mockOrder.setCreatedAt(LocalDateTime.now());
            mockOrder.setNote("Extra hot please");

            // 2. Mock the repository to return this order when findById is called
            when(orderRepository.findById(1)).thenReturn(Optional.of(mockOrder));

            // === When ===
            ResponseEntity<?> responseEntity = orderService.getTransactionDetail(1, testUser.getUserId());

            // === Then ===
            // 1. Verify repository interaction
            verify(orderRepository).findById(1);

            // 2. Assert response status and body
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isInstanceOf(Map.class);

            // 3. Deeply assert the contents of the response body
            Map<String, Object> body = (Map<String, Object>) responseEntity.getBody();
            List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
            assertThat(data).hasSize(1);

            Map<String, Object> detail = data.get(0);
            assertThat(detail.get("id")).isEqualTo(1);
            assertThat(detail.get("receiver_name")).isEqualTo("Kopi Customer");
            assertThat(detail.get("delivery_address")).isEqualTo("123 Kopi Lane");
            assertThat(detail.get("notes")).isEqualTo("Extra hot please");
            assertThat(detail.get("status_name")).isEqualTo("COMPLETED");
            assertThat(detail.get("payment_name")).isEqualTo("CASH");
            assertThat(detail.get("grand_total")).isEqualTo(new BigDecimal("4.00"));

            List<Map<String, Object>> products = (List<Map<String, Object>>) detail.get("products");
            assertThat(products).hasSize(1);
        }

        @Test
        @DisplayName("should_ReturnOrderWithMultipleProducts_When_OrderHasManyDetails")
        void should_ReturnOrderWithMultipleProducts_When_OrderHasManyDetails() {
            // === Given ===
            Product p1 = new Product();
            p1.setName("Latte");
            Product p2 = new Product();
            p2.setName("Muffin");

            OrderDetail od1 = new OrderDetail();
            od1.setProduct(p1);
            OrderDetail od2 = new OrderDetail();
            od2.setProduct(p2);

            OrderEntity mockOrder = new OrderEntity();
            mockOrder.setOrderId(2);
            mockOrder.setCustomer(testUser);
            mockOrder.setOrderDetails(List.of(od1, od2)); // The critical condition

            when(orderRepository.findById(2)).thenReturn(Optional.of(mockOrder));

            // === When ===
            ResponseEntity<?> responseEntity = orderService.getTransactionDetail(2, testUser.getUserId());

            // === Then ===
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> body = (Map<String, Object>) responseEntity.getBody();
            List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
            Map<String, Object> detail = data.get(0);

            List<Map<String, Object>> products = (List<Map<String, Object>>) detail.get("products");
            assertThat(products).hasSize(2);
        }

        @Test
        @DisplayName("should_ReturnOrderWithEmptyProductList_When_OrderHasNoDetails")
        void should_ReturnOrderWithEmptyProductList_When_OrderHasNoDetails() {
            // === Given ===
            OrderEntity mockOrder = new OrderEntity();
            mockOrder.setOrderId(3);
            mockOrder.setCustomer(testUser);
            mockOrder.setOrderDetails(null); // The critical condition

            when(orderRepository.findById(3)).thenReturn(Optional.of(mockOrder));

            // === When ===
            ResponseEntity<?> responseEntity = orderService.getTransactionDetail(3, testUser.getUserId());

            // === Then ===
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> body = (Map<String, Object>) responseEntity.getBody();
            List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
            Map<String, Object> detail = data.get(0);

            List<Map<String, Object>> products = (List<Map<String, Object>>) detail.get("products");
            assertThat(products).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("should_ThrowNoSuchElementException_When_OrderNotFound")
        void should_ThrowNoSuchElementException_When_OrderNotFound() {
            // === Given ===
            int nonExistentId = 999;
            // Mock repository to return an empty Optional, simulating a not-found scenario
            when(orderRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // === When & Then ===
            // The service method calls .orElseThrow(), so we expect this exception
            assertThrows(NoSuchElementException.class, () -> {
                orderService.getTransactionDetail(nonExistentId, testUser.getUserId());
            });

            verify(orderRepository).findById(nonExistentId);
        }

        @Test
        @DisplayName("should_ReturnCorrectStatus_When_RetrievingPendingOrder")
        void should_ReturnCorrectStatus_When_RetrievingPendingOrder() {
            // === Given ===
            OrderEntity mockOrder = new OrderEntity();
            mockOrder.setOrderId(5);
            mockOrder.setCustomer(testUser);
            mockOrder.setStatus("PENDING"); // The critical condition
            mockOrder.setOrderDetails(new ArrayList<>());

            when(orderRepository.findById(5)).thenReturn(Optional.of(mockOrder));

            // === When ===
            ResponseEntity<?> responseEntity = orderService.getTransactionDetail(5, testUser.getUserId());

            // === Then ===
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            Map<String, Object> body = (Map<String, Object>) responseEntity.getBody();
            List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
            Map<String, Object> detail = data.get(0);

            assertThat(detail.get("status_name")).isEqualTo("PENDING");
        }



        @Test
        @DisplayName("should_ReturnFirstPage_When_RequestingPendingOrders")
        void should_ReturnFirstPage_When_RequestingPendingOrders() {
            // === Given ===
            // 1. Create mock data for the page content
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
            long totalElements = 12; // Simulate a total of 12 orders, so more pages are available

            // 2. Create the mock Page object that the repository will return
            Page<OrderEntity> orderPage = new PageImpl<>(pageContent, requestedPageable, totalElements);
            when(orderRepository.findByStatus(eq("PENDING"), any(Pageable.class))).thenReturn(orderPage);

            // === When ===
            Map<String, Object> result = orderService.listPending("PENDING", null, 1, 5);

            // === Then ===
            // 1. Verify the repository was called with the correct arguments
            verify(orderRepository).findByStatus("PENDING", PageRequest.of(0, 5));

            // 2. Assert the response data
            assertThat(result).isNotNull();
            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
            assertThat(data).hasSize(2);
            assertThat(data.get(0).get("id")).isEqualTo(10);
            assertThat(data.get(0).get("status")).isEqualTo("PENDING");
            assertThat(data.get(0).get("address")).isEqualTo("456 Admin Avenue");
            assertThat(data.get(1).get("address")).isNull(); // Verify null address handling

            // 3. Assert the pagination metadata
            Map<String, Object> meta = (Map<String, Object>) result.get("meta");
            assertThat(meta.get("currentPage")).isEqualTo(1);
            assertThat(meta.get("totalPage")).isEqualTo(3); // 12 items / 5 per page = 2.4 -> 3 pages
            assertThat(meta.get("prev")).isEqualTo(false);
            assertThat(meta.get("next")).isEqualTo(true);
        }

        @Test
        @DisplayName("should_ReturnLastPageWithCorrectMeta_When_RequestingFinalPage")
        void should_ReturnLastPageWithCorrectMeta_When_RequestingFinalPage() {
            // === Given ===
            OrderEntity order1 = new OrderEntity();
            order1.setOrderId(20);
            order1.setStatus("COMPLETED");

            List<OrderEntity> lastPageContent = List.of(order1);
            Pageable requestedPageable = PageRequest.of(2, 10);
            long totalElements = 21; // Total of 21 orders, 10 per page, so page 3 is the last one

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
            assertThat(meta.get("next")).isEqualTo(false); // Crucial assertion for the last page
        }

        @Test
        @DisplayName("should_ReturnOrderWithFormattedProducts_When_OrderHasMultipleDetails")
        void should_ReturnOrderWithFormattedProducts_When_OrderHasMultipleDetails() {
            // === Given ===
            // 1. Create mock order details with snapshots
            OrderDetail detail1 = new OrderDetail();
            detail1.setProductNameSnapshot("Americano");
            detail1.setQuantity(2);
            detail1.setLineTotal(new BigDecimal("5.00"));

            OrderDetail detail2 = new OrderDetail();
            detail2.setProductNameSnapshot("Croissant");
            detail2.setQuantity(1);
            detail2.setLineTotal(new BigDecimal("2.50"));

            // 2. Create the mock order containing these details
            OrderEntity mockOrder = new OrderEntity();
            mockOrder.setOrderId(30);
            mockOrder.setStatus("PENDING");
            mockOrder.setOrderDetails(List.of(detail1, detail2)); // The critical condition

            Page<OrderEntity> orderPage = new PageImpl<>(List.of(mockOrder));
            when(orderRepository.findByStatus(eq("PENDING"), any(Pageable.class))).thenReturn(orderPage);

            // === When ===
            Map<String, Object> result = orderService.listPending("PENDING", null, 1, 5);

            // === Then ===
            verify(orderRepository).findByStatus("PENDING", PageRequest.of(0, 5));

            List<Map<String, Object>> data = (List<Map<String, Object>>) result.get("data");
            assertThat(data).hasSize(1);

            // 3. Deeply assert the products list within the order
            Map<String, Object> orderMap = data.get(0);
            List<Map<String, Object>> products = (List<Map<String, Object>>) orderMap.get("products");
            assertThat(products).hasSize(2);

            // 4. Assert the content of the first product
            Map<String, Object> product1Map = products.get(0);
            assertThat(product1Map.get("product_name")).isEqualTo("Americano");
            assertThat(product1Map.get("qty")).isEqualTo(2);
            assertThat(product1Map.get("subtotal")).isEqualTo(new BigDecimal("5.00"));
        }


}