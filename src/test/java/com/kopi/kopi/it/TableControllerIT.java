package com.kopi.kopi.it;

import com.kopi.kopi.controller.TableController;
import com.kopi.kopi.service.TableService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.kopi.kopi.security.JwtAuthenticationFilter;
import com.kopi.kopi.security.JwtTokenProvider;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(controllers = TableController.class)
@AutoConfigureMockMvc(addFilters = false)
class TableControllerIT {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TableService tableService;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @DisplayName("GET /apiv1/tables/by-qr/{qrToken} -> 200 and returns table")
    void getByQr_ok() throws Exception {
        // Khóa kiểu trả về cho Mockito để tránh lỗi generic
        org.mockito.stubbing.OngoingStubbing<ResponseEntity<?>> stubbing =
                when(tableService.getByQr(anyString()));

        stubbing.thenReturn(ResponseEntity.ok(Map.of(
                "tableId", 1,
                "qrToken", "abc123",
                "status", "ACTIVE"
        )));

        mvc.perform(get("/apiv1/tables/by-qr/{qrToken}", "abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.qrToken").value("abc123"))
                .andExpect(jsonPath("$.tableId").value(1));
    }
}
