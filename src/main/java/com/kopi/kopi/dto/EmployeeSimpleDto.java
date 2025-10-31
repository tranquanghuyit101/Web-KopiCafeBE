package com.kopi.kopi.dto;

/**
 * Minimal DTO for admin employee listing used by the admin UI.
 */
public record EmployeeSimpleDto(
                Integer userId,
                String fullName,
                String positionName,
                String status) {
}
