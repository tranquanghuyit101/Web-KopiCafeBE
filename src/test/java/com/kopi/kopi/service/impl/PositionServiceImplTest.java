package com.kopi.kopi.service.impl;

import com.kopi.kopi.dto.PositionDto;
import com.kopi.kopi.entity.Position;
import com.kopi.kopi.repository.PositionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionServiceImplTest {

    @Mock
    private PositionRepository positionRepository;

    private PositionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PositionServiceImpl(positionRepository);
    }

    @AfterEach
    void tearDown() {
        // no-op
    }

    private Position pos(Integer id, String name) {
        Position p = new Position();
        p.setPositionId(id);
        p.setPositionName(name);
        p.setActive(true);
        return p;
    }

    @Test
    void should_ReturnMappedDtos_InOrder_When_ActivePositionsExist() {
        // Given
        var p1 = pos(1, "Barista");
        var p2 = pos(2, "Cashier");
        when(positionRepository.findByIsActiveTrue()).thenReturn(List.of(p1, p2));

        // When
        List<PositionDto> dtos = service.listPositions();

        // Then
        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).positionId()).isEqualTo(1);
        assertThat(dtos.get(0).positionName()).isEqualTo("Barista");
        assertThat(dtos.get(1).positionId()).isEqualTo(2);
        assertThat(dtos.get(1).positionName()).isEqualTo("Cashier");
        verify(positionRepository, times(1)).findByIsActiveTrue();
    }

    @Test
    void should_ReturnEmptyList_When_NoActivePositions() {
        // Given
        when(positionRepository.findByIsActiveTrue()).thenReturn(List.of());

        // When
        List<PositionDto> dtos = service.listPositions();

        // Then
        assertThat(dtos).isEmpty();
        verify(positionRepository).findByIsActiveTrue();
    }

    @Test
    void should_MapNullName_When_PositionNameIsNull() {
        // Given
        var p = pos(3, null);
        when(positionRepository.findByIsActiveTrue()).thenReturn(List.of(p));

        // When
        List<PositionDto> dtos = service.listPositions();

        // Then
        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).positionId()).isEqualTo(3);
        assertThat(dtos.get(0).positionName()).isNull();
    }

    @Test
    void should_CallRepositoryOnce_Only() {
        // Given
        when(positionRepository.findByIsActiveTrue()).thenReturn(List.of());

        // When
        service.listPositions();

        // Then
        verify(positionRepository, times(1)).findByIsActiveTrue();
        verifyNoMoreInteractions(positionRepository);
    }
}
