package com.kopi.kopi.dto;

/**
 * Detailed employee information for admin UI.
 */
public record EmployeeDetailDto(
        Integer userId,
        String username,
        String email,
        String phone,
        String street,
        String city,
        String district) {
}
