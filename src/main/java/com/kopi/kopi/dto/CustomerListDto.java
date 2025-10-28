package com.kopi.kopi.dto;

/**
 * DTO for listing customers in admin UI (paginated).
 */
public record CustomerListDto(
        Integer userId,
        String fullName,
        String status,
        String roleName) {
}
