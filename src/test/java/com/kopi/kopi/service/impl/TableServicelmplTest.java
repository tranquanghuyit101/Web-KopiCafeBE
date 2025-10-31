package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.DiningTable;
import com.kopi.kopi.repository.DiningTableRepository;
import com.kopi.kopi.repository.OrderRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableServiceImplTest {

    @Mock private DiningTableRepository diningTableRepository;
    @Mock private OrderRepository orderRepository;

    private TableServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TableServiceImpl(diningTableRepository, orderRepository);
    }

    // ---------- helpers ----------
    private DiningTable table(Integer id, Integer number, String name, String status) {
        return DiningTable.builder()
                .tableId(id)
                .number(number)
                .name(name)
                .status(status)
                .qrToken("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa")
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    // ================= setOccupiedIfHasPendingOrders =================

    @Test
    void should_SetOccupied_When_PendingExists_AndStatusDiffers() {
        // Given
        when(orderRepository.countByTable_TableIdAndStatus(1, "PENDING")).thenReturn(5L);
        DiningTable t = table(1, 10, "T1", "AVAILABLE");
        when(diningTableRepository.findById(1)).thenReturn(Optional.of(t));

        // When
        service.setOccupiedIfHasPendingOrders(1);

        // Then
        assertThat(t.getStatus()).isEqualTo("OCCUPIED");
        assertThat(t.getUpdatedAt()).isNotNull();
        verify(diningTableRepository).save(t);
    }

    @Test
    void should_SetAvailable_When_NoPending_ButWasOccupied() {
        // Given
        when(orderRepository.countByTable_TableIdAndStatus(2, "PENDING")).thenReturn(0L);
        DiningTable t = table(2, 11, "T2", "OCCUPIED");
        when(diningTableRepository.findById(2)).thenReturn(Optional.of(t));

        // When
        service.setOccupiedIfHasPendingOrders(2);

        // Then
        assertThat(t.getStatus()).isEqualTo("AVAILABLE");
        verify(diningTableRepository).save(t);
    }

    @Test
    void should_NotSave_When_StatusAlreadyCorrect() {
        // Given
        when(orderRepository.countByTable_TableIdAndStatus(3, "PENDING")).thenReturn(7L);
        DiningTable t = table(3, 12, "T3", "OCCUPIED");
        when(diningTableRepository.findById(3)).thenReturn(Optional.of(t));

        // When
        service.setOccupiedIfHasPendingOrders(3);

        // Then
        verify(diningTableRepository, never()).save(any());
    }

    @Test
    void should_DoNothing_When_TableNotFound_InSetOccupied() {
        // Given
        when(orderRepository.countByTable_TableIdAndStatus(404, "PENDING")).thenReturn(1L);
        when(diningTableRepository.findById(404)).thenReturn(Optional.empty());

        // When
        service.setOccupiedIfHasPendingOrders(404);

        // Then
        verify(diningTableRepository, never()).save(any());
    }

    // ================= setAvailableIfNoPendingOrders =================

    @Test
    void should_SetAvailable_When_NoPending_AndNotAvailable() {
        // Given
        when(orderRepository.countByTable_TableIdAndStatus(5, "PENDING")).thenReturn(0L);
        DiningTable t = table(5, 15, "T5", "OCCUPIED");
        when(diningTableRepository.findById(5)).thenReturn(Optional.of(t));

        // When
        service.setAvailableIfNoPendingOrders(5);

        // Then
        assertThat(t.getStatus()).isEqualTo("AVAILABLE");
        verify(diningTableRepository).save(t);
    }

    @Test
    void should_NotSave_When_NoPending_AndAlreadyAvailable() {
        // Given
        when(orderRepository.countByTable_TableIdAndStatus(6, "PENDING")).thenReturn(0L);
        DiningTable t = table(6, 16, "T6", "AVAILABLE");
        when(diningTableRepository.findById(6)).thenReturn(Optional.of(t));

        // When
        service.setAvailableIfNoPendingOrders(6);

        // Then
        verify(diningTableRepository, never()).save(any());
    }

    @Test
    void should_NotQueryTable_When_PendingExists_InSetAvailable() {
        // Given
        when(orderRepository.countByTable_TableIdAndStatus(7, "PENDING")).thenReturn(3L);

        // When
        service.setAvailableIfNoPendingOrders(7);

        // Then
        verify(diningTableRepository, never()).findById(anyInt());
        verify(diningTableRepository, never()).save(any());
    }

    @Test
    void should_NotSave_When_NoPending_But_TableNotFound() {
        // Given
        when(orderRepository.countByTable_TableIdAndStatus(8, "PENDING")).thenReturn(0L);
        when(diningTableRepository.findById(8)).thenReturn(Optional.empty());

        // When
        service.setAvailableIfNoPendingOrders(8);

        // Then
        verify(diningTableRepository, never()).save(any());
    }

    // ================= list =================

    @Test
    void should_ListAll_When_StatusBlank() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<DiningTable> pg = new PageImpl<>(List.of(table(1,10,"A","AVAILABLE")), pageable, 1);
        when(diningTableRepository.findAll(any(Pageable.class))).thenReturn(pg);

        // When
        Map<String,Object> res = service.list(1, 10, "");

        // Then
        verify(diningTableRepository).findAll(any(Pageable.class));
        Map<String,Object> meta = (Map<String,Object>) res.get("meta");
        assertThat(meta.get("currentPage")).isEqualTo(1);
        assertThat(meta.get("totalPage")).isEqualTo(1);
        assertThat(meta.get("prev")).isEqualTo(false);
        assertThat(meta.get("next")).isEqualTo(false);
    }

    @Test
    void should_ListByStatus_When_StatusProvided() {
        // Given
        Pageable pageable = PageRequest.of(0, 5);
        Page<DiningTable> pg = new PageImpl<>(List.of(table(2,11,"B","OCCUPIED")), pageable, 1);
        when(diningTableRepository.findByStatus(eq("OCCUPIED"), any(Pageable.class))).thenReturn(pg);

        // When
        Map<String,Object> res = service.list(1, 5, "OCCUPIED");

        // Then
        verify(diningTableRepository).findByStatus(eq("OCCUPIED"), any(Pageable.class));
        assertThat(((List<?>) res.get("data"))).hasSize(1);
    }

    @Test
    void should_NormalizePageAndLimit() {
        // Given
        Page<DiningTable> pg = new PageImpl<>(List.of(), PageRequest.of(0,1), 0);
        when(diningTableRepository.findAll(any(Pageable.class))).thenReturn(pg);

        // When
        service.list(0, 0, null);

        // Then
        ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
        verify(diningTableRepository).findAll(cap.capture());
        assertThat(cap.getValue().getPageNumber()).isZero();
        assertThat(cap.getValue().getPageSize()).isEqualTo(1);
    }

    // ================= create =================

    @Test
    void should_Create_WithQrToken_AndDefaultStatusAvailable() {
        // Given
        Map<String,Object> body = new HashMap<>();
        body.put("number", 12);
        body.put("name", "Vip 1");
        // no status -> default AVAILABLE

        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(inv -> {
            DiningTable t = inv.getArgument(0);
            t.setTableId(99);
            return t;
        });

        // When
        ResponseEntity<?> resp = service.create(body);

        // Then
        Map<?,?> out = (Map<?,?>) ((Map<?,?>) resp.getBody()).get("data");
        DiningTable saved = (DiningTable) ((Map<?,?>) resp.getBody()).get("data"); // service trả t trực tiếp
        assertThat(saved.getTableId()).isEqualTo(99);
        assertThat(saved.getStatus()).isEqualTo("AVAILABLE");
        assertThat(saved.getQrToken()).matches("^[0-9a-f]{32}$");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        verify(diningTableRepository).save(any(DiningTable.class));
    }

    @Test
    void should_ParseNumber_When_NumberIsString() {
        // Given
        Map<String,Object> body = Map.of("number", "15", "status", "OCCUPIED");
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ResponseEntity<?> resp = service.create(body);

        // Then
        DiningTable saved = (DiningTable) ((Map<?,?>) resp.getBody()).get("data");
        assertThat(saved.getNumber()).isEqualTo(15);
        assertThat(saved.getStatus()).isEqualTo("OCCUPIED");
    }

    // ================= patch =================

    @Test
    void should_Patch_Number_Name_Status_AndUpdateTimestamp() {
        // Given
        DiningTable t = table(5, 10, "A", "AVAILABLE");
        when(diningTableRepository.findById(5)).thenReturn(Optional.of(t));

        Map<String,Object> body = new HashMap<>();
        body.put("number", 20);
        body.put("name", "New");
        body.put("status", "OCCUPIED");

        // When
        ResponseEntity<?> resp = service.patch(5, body);

        // Then
        verify(diningTableRepository).save(t);
        DiningTable patched = (DiningTable) ((Map<?,?>) resp.getBody()).get("data");
        assertThat(patched.getNumber()).isEqualTo(20);
        assertThat(patched.getName()).isEqualTo("New");
        assertThat(patched.getStatus()).isEqualTo("OCCUPIED");
        assertThat(patched.getUpdatedAt()).isNotNull();
    }

    @Test
    void should_SetNameNull_When_NameKeyPresentWithNull() {
        // Given
        DiningTable t = table(6, 11, "Old", "AVAILABLE");
        when(diningTableRepository.findById(6)).thenReturn(Optional.of(t));

        Map<String,Object> body = new HashMap<>();
        body.put("name", null);

        // When
        service.patch(6, body);

        // Then
        assertThat(t.getName()).isNull();
        verify(diningTableRepository).save(t);
    }

    @Test
    void should_Save_When_BodyEmpty_JustUpdateTimestamp() {
        // Given
        DiningTable t = table(7, 12, "X", "AVAILABLE");
        when(diningTableRepository.findById(7)).thenReturn(Optional.of(t));

        // When
        service.patch(7, Map.of());

        // Then
        verify(diningTableRepository).save(t);
        assertThat(t.getUpdatedAt()).isNotNull();
    }

    @Test
    void should_Throw_When_TableNotFound_OnPatch() {
        // Given
        when(diningTableRepository.findById(404)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.patch(404, Map.of()))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ================= delete =================

    @Test
    void should_DeleteById_AndReturnOk() {
        // When
        ResponseEntity<?> resp = service.delete(10);

        // Then
        verify(diningTableRepository, times(1)).deleteById(10);
        assertThat(((Map<?,?>) resp.getBody()).get("message")).isEqualTo("OK");
    }

    // ================= rotateQr =================

    @Test
    void should_RotateQr_GenerateNewToken_Save_AndReturnPayload() {
        // Given
        DiningTable t = table(1, 10, "A", "AVAILABLE");
        t.setQrToken("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        when(diningTableRepository.findById(1)).thenReturn(Optional.of(t));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ResponseEntity<?> resp = service.rotateQr(1);

        // Then
        Map<String, Object> body = (Map<String, Object>) resp.getBody();
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        assertThat(data).containsKeys("id", "number", "qr_token");
        String newToken = (String) data.get("qr_token");
        assertThat(newToken).matches("^[0-9a-f]{32}$");
        assertThat(newToken).isNotEqualTo("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        verify(diningTableRepository).save(t);
    }

    @Test
    void should_Throw_When_TableNotFound_OnRotateQr() {
        // Given
        when(diningTableRepository.findById(999)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.rotateQr(999))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ================= getByQr =================

    @Test
    void should_Return200_WithData_When_QrFound() {
        // Given
        DiningTable t = table(2, 22, "Vip", "OCCUPIED");
        when(diningTableRepository.findByQrToken("tok")).thenReturn(Optional.of(t));

        // When
        ResponseEntity<?> resp = service.getByQr("tok");

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        Map<String,Object> data = (Map<String,Object>) ((Map<?,?>) resp.getBody()).get("data");
        assertThat(data.get("id")).isEqualTo(2);
        assertThat(data.get("number")).isEqualTo(22);
        assertThat(data.get("status")).isEqualTo("OCCUPIED");
        verify(diningTableRepository, never()).save(any());
    }

    @Test
    void should_Return404_When_QrNotFound() {
        // Given
        when(diningTableRepository.findByQrToken("none")).thenReturn(Optional.empty());

        // When
        ResponseEntity<?> resp = service.getByQr("none");

        // Then
        assertThat(resp.getStatusCodeValue()).isEqualTo(404);
        verify(diningTableRepository, never()).save(any());
    }
}
