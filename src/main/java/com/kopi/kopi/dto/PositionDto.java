package com.kopi.kopi.dto;

/**
 * DTO for exposing positions to frontend
 */
public record PositionDto(
        Integer positionId,
        String positionName) {
}
