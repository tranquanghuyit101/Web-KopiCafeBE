package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.*;
import com.kopi.kopi.repository.AddressRepository;
import com.kopi.kopi.repository.DiningTableRepository;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.repository.ProductRepository;
import com.kopi.kopi.repository.UserAddressRepository;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.service.TableService;
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

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAddressRepository userAddressRepository;

    @Mock
    private TableService tableService;

    @Mock
    private DiningTableRepository diningTableRepository;

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
    @DisplayName("Feature: Get Transaction Detail")
    class GetTransactionDetailTests {

        @Test
        @DisplayName("should Return 200 And Detail When User Owns Order")
        void should_Return200AndDetail_When_UserOwnsOrder() {
            // === Given ===
            User customer = new User();
            customer.setUserId(42);
            customer.setFullName("Alice");

            Address addr = new Address();
            addr.setAddressLine("123 Street");

            Product prod = new Product();
            prod.setName("Espresso");
            prod.setImgUrl("img");

            OrderDetail d = new OrderDetail();
            d.setOrderDetailId(1001);
            d.setProduct(prod);
            d.setProductNameSnapshot("Espresso");
            d.setQuantity(1);
            d.setLineTotal(new BigDecimal("3.50"));

            Payment pay = new Payment();
            pay.setMethod(com.kopi.kopi.entity.enums.PaymentMethod.CASH);

            OrderEntity order = new OrderEntity();
            order.setOrderId(500);
            order.setCustomer(customer);
            order.setAddress(addr);
            order.setNote("note");
            order.setStatus("PENDING");
            order.setCreatedAt(LocalDateTime.now());
            order.setTotalAmount(new BigDecimal("3.50"));
            order.setOrderDetails(List.of(d));
            order.setPayments(new ArrayList<>(List.of(pay)));

            when(orderRepository.findById(eq(500))).thenReturn(java.util.Optional.of(order));

            // === When ===
            var resp = orderService.getTransactionDetail(500, 42);

            // === Then ===
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            Map<String, Object> body = (Map<String, Object>) ((List<?>) ((Map<?, ?>) resp.getBody()).get("data"))
                    .get(0);
            assertThat(body.get("id")).isEqualTo(500);
            assertThat(body.get("receiver_name")).isEqualTo("Alice");
            assertThat(body.get("delivery_address")).isEqualTo("123 Street");
            assertThat(body.get("payment_name")).isEqualTo("CASH");
            assertThat(body.get("grand_total")).isEqualTo(new BigDecimal("3.50"));
            List<?> products = (List<?>) body.get("products");
            assertThat(products).hasSize(1);
        }

        @Test
        @DisplayName("should Return 403 When User Not Owner")
        void should_Return403_When_UserNotOwner() {
            // === Given ===
            User someoneElse = new User();
            someoneElse.setUserId(7);

            OrderEntity order = new OrderEntity();
            order.setOrderId(501);
            order.setCustomer(someoneElse);

            when(orderRepository.findById(eq(501))).thenReturn(java.util.Optional.of(order));

            // === When ===
            var resp = orderService.getTransactionDetail(501, 42);

            // === Then ===
            assertThat(resp.getStatusCode().value()).isEqualTo(403);
        }
    }

    @Nested
    @DisplayName("Feature: Change Status")
    class ChangeStatusTests {

        @Test
        @DisplayName("should Return 400 When Status Invalid")
        void should_Return400_When_StatusInvalid() {
            // === When ===
            var resp = orderService.changeStatus(1, Map.of("status", "UNKNOWN"));
            // === Then ===
            assertThat(resp.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("should Update Order And Payment To PAID When Completed")
        void should_UpdateOrderAndPayment_When_Completed() {
            // === Given ===
            Payment payment = new Payment();
            payment.setStatus(com.kopi.kopi.entity.enums.PaymentStatus.PENDING);

            OrderEntity order = new OrderEntity();
            order.setOrderId(10);
            order.setStatus("PENDING");
            order.setPayments(new ArrayList<>(List.of(payment)));

            when(orderRepository.findById(eq(10))).thenReturn(java.util.Optional.of(order));

            // === When ===
            var resp = orderService.changeStatus(10, Map.of("status", "COMPLETED"));

            // === Then ===
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            assertThat(order.getStatus()).isEqualTo("COMPLETED");
            assertThat(order.getPayments().get(0).getStatus()).isEqualTo(com.kopi.kopi.entity.enums.PaymentStatus.PAID);
            verify(orderRepository).save(any(OrderEntity.class));
        }

        @Test
        @DisplayName("should Free Table When Order Has Table")
        void should_FreeTable_When_OrderHasTable() {
            // === Given ===
            DiningTable table = new DiningTable();
            table.setTableId(99);
            OrderEntity order = new OrderEntity();
            order.setOrderId(11);
            order.setStatus("PENDING");
            order.setPayments(new ArrayList<>());
            order.setTable(table);

            when(orderRepository.findById(eq(11))).thenReturn(java.util.Optional.of(order));

            // === When ===
            orderService.changeStatus(11, Map.of("status", "COMPLETED"));

            // === Then ===
            verify(tableService).setAvailableIfNoPendingOrders(eq(99));
        }
    }

    @Nested
    @DisplayName("Feature: Create Transaction")
    class CreateTransactionTests {

        @Test
        @DisplayName("should Create For Customer Role With Address And Pending Payment")
        void should_CreateForCustomerRole_WithAddress() {
            // === Given ===
            User current = new User();
            current.setUserId(5);
            Role role = new Role();
            role.setRoleId(3); // customer
            current.setRole(role);

            Product prod = new Product();
            prod.setProductId(1);
            prod.setName("Latte");
            prod.setPrice(new BigDecimal("2.50"));
            prod.setStockQty(100);

            when(productRepository.findById(eq(1))).thenReturn(java.util.Optional.of(prod));
            when(userRepository.findById(eq(5))).thenReturn(java.util.Optional.of(current));
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
                OrderEntity o = inv.getArgument(0);
                o.setOrderId(123);
                return o;
            });

            Map<String, Object> body = Map.of(
                    "products", List.of(Map.of("product_id", 1, "qty", 2)),
                    "notes", "note",
                    "address", "Addr",
                    "payment_id", 1,
                    "paid", false);

            // === When ===
            var resp = orderService.createTransaction(body, current);

            // === Then ===
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            Map<?, ?> data = (Map<?, ?>) ((Map<?, ?>) resp.getBody()).get("data");
            assertThat(data.get("id")).isEqualTo(123);
        }

        @Test
        @DisplayName("should Create For Staff Role With CustomerId")
        void should_CreateForStaffRole_WithCustomerId() {
            // === Given ===
            User staff = new User();
            staff.setUserId(2);
            Role role = new Role();
            role.setRoleId(2); // staff
            staff.setRole(role);

            User customer = new User();
            customer.setUserId(9);

            Product prod = new Product();
            prod.setProductId(1);
            prod.setName("Latte");
            prod.setPrice(new BigDecimal("2.00"));
            prod.setStockQty(100);

            when(productRepository.findById(eq(1))).thenReturn(java.util.Optional.of(prod));
            when(userRepository.findById(eq(2))).thenReturn(java.util.Optional.of(staff));
            when(userRepository.findById(eq(9))).thenReturn(java.util.Optional.of(customer));
            when(addressRepository.save(any(Address.class))).thenAnswer(inv -> inv.getArgument(0));
            when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
                OrderEntity o = inv.getArgument(0);
                o.setOrderId(321);
                return o;
            });

            Map<String, Object> body = Map.of(
                    "products", List.of(Map.of("product_id", 1, "qty", 1)),
                    "notes", "note",
                    "address", "Addr",
                    "payment_id", 2,
                    "customer_id", 9,
                    "paid", true);

            // === When ===
            var resp = orderService.createTransaction(body, staff);

            // === Then ===
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            Map<?, ?> data = (Map<?, ?>) ((Map<?, ?>) resp.getBody()).get("data");
            assertThat(data.get("id")).isEqualTo(321);
        }
    }

    @Nested
    @DisplayName("Feature: Create Guest Table Order")
    class CreateGuestTableOrderTests {

        @Test
        @DisplayName("should Return 400 When No Token And No TableNumber")
        void should_Return400_When_NoTokenAndNoTable() {
            // === When ===
            var req = new com.kopi.kopi.controller.GuestOrderController.GuestOrderRequest(null, null, List.of(), null,
                    1, false);
            var resp = orderService.createGuestTableOrder(req);
            // === Then ===
            assertThat(resp.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("should Return 404 When Table Not Found")
        void should_Return404_When_TableNotFound() {
            // === Given ===
            when(diningTableRepository.findByQrToken(eq("abc"))).thenReturn(java.util.Optional.empty());
            when(diningTableRepository.findByNumber(eq(1))).thenReturn(java.util.Optional.empty());
            var req = new com.kopi.kopi.controller.GuestOrderController.GuestOrderRequest("abc", 1,
                    List.of(new com.kopi.kopi.controller.GuestOrderController.GuestOrderItem(1, 1)), null, 1, false);
            // === When ===
            var resp = orderService.createGuestTableOrder(req);
            // === Then ===
            assertThat(resp.getStatusCode().value()).isEqualTo(404);
        }

        @Test
        @DisplayName("should Return 400 When Table Disabled")
        void should_Return400_When_TableDisabled() {
            // === Given ===
            DiningTable t = new DiningTable();
            t.setStatus("DISABLED");
            when(diningTableRepository.findByQrToken(eq("abc"))).thenReturn(java.util.Optional.of(t));
            var req = new com.kopi.kopi.controller.GuestOrderController.GuestOrderRequest("abc", null,
                    List.of(new com.kopi.kopi.controller.GuestOrderController.GuestOrderItem(1, 1)), null, 1, false);
            // === When ===
            var resp = orderService.createGuestTableOrder(req);
            // === Then ===
            assertThat(resp.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("should Return 400 When No Products")
        void should_Return400_When_NoProducts() {
            // === Given ===
            DiningTable t = new DiningTable();
            t.setStatus("AVAILABLE");
            when(diningTableRepository.findByQrToken(eq("abc"))).thenReturn(java.util.Optional.of(t));
            var req = new com.kopi.kopi.controller.GuestOrderController.GuestOrderRequest("abc", null, List.of(), null,
                    1, false);
            // === When ===
            var resp = orderService.createGuestTableOrder(req);
            // === Then ===
            assertThat(resp.getStatusCode().value()).isEqualTo(400);
        }

        @Test
        @DisplayName("should Create Guest Order And Mark Table Occupied When Pending")
        void should_CreateGuestOrder_And_MarkTableOccupied() {
            // === Given ===
            DiningTable t = new DiningTable();
            t.setTableId(10);
            t.setNumber(7);
            t.setStatus("AVAILABLE");
            when(diningTableRepository.findByQrToken(eq("abc"))).thenReturn(java.util.Optional.of(t));

            Product p = new Product();
            p.setProductId(1);
            p.setName("Americano");
            p.setPrice(new BigDecimal("2.00"));
            p.setStockQty(100);
            when(productRepository.findById(eq(1))).thenReturn(java.util.Optional.of(p));

            when(orderRepository.save(any(OrderEntity.class))).thenAnswer(inv -> {
                OrderEntity o = inv.getArgument(0);
                o.setOrderId(777);
                return o;
            });

            var items = List.of(new com.kopi.kopi.controller.GuestOrderController.GuestOrderItem(1, 2));
            var req = new com.kopi.kopi.controller.GuestOrderController.GuestOrderRequest("abc", null, items, "note", 1,
                    false);

            // === When ===
            var resp = orderService.createGuestTableOrder(req);

            // === Then ===
            assertThat(resp.getStatusCode().value()).isEqualTo(200);
            Map<?, ?> data = (Map<?, ?>) ((Map<?, ?>) resp.getBody()).get("data");
            assertThat(data.get("id")).isEqualTo(777);
            assertThat(data.get("table_number")).isEqualTo(7);
            assertThat(data.get("status")).isEqualTo("PENDING");
            verify(tableService).setOccupiedIfHasPendingOrders(eq(10));
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
            verify(orderRepository).findByCustomer_UserId(eq(101), any(Pageable.class));

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
            verify(orderRepository).findByCustomer_UserId(eq(101), any(Pageable.class));

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