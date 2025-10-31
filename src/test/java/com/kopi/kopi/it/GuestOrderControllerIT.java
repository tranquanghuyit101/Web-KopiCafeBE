package com.kopi.kopi.it;

import com.kopi.kopi.controller.GuestOrderController;
import com.kopi.kopi.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.kopi.kopi.security.JwtAuthenticationFilter;
import com.kopi.kopi.security.JwtTokenProvider;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = GuestOrderController.class)
@AutoConfigureMockMvc(addFilters = false)
class GuestOrderControllerIT {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("POST /apiv1/guest/table-orders -> 200 and return orderId")
    void guestCreate_ok() throws Exception {
        org.mockito.stubbing.OngoingStubbing<ResponseEntity<?>> stubbing =
                when(orderService.createGuestTableOrder(any(GuestOrderController.GuestOrderRequest.class)));

        stubbing.thenReturn(ResponseEntity.ok(Map.of(
                "orderId", 99,
                "status", "PENDING"
        )));

        String body = """
        {
          "tableQr": "abc123",
          "customerName": "Guest",
          "products": [ { "productId": 1, "quantity": 2 } ]
        }
        """;

        mvc.perform(post("/apiv1/guest/table-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(99))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
