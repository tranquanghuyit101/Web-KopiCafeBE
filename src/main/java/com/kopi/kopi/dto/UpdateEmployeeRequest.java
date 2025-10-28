package com.kopi.kopi.dto;

/**
 * Request payload used by admin to update employee role/status/position.
 */
public record UpdateEmployeeRequest(
        Integer roleId,
        Integer positionId,
        String status) {
}
