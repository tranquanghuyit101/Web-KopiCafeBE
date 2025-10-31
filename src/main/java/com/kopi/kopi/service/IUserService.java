package com.kopi.kopi.service;
import org.springframework.security.access.prepost.PreAuthorize;

public interface IUserService {
    @PreAuthorize("permitAll()")
    void resetPassword(String email);

    @PreAuthorize("permitAll()")
    void changePassword(String email, String newPassword);

    boolean mustChangePassword(String email);
    void clearForceChangePassword(String email);
    java.util.List<com.kopi.kopi.dto.EmployeeSimpleDto> listEmployees();

    java.util.List<com.kopi.kopi.dto.EmployeeSimpleDto> searchEmployees(
            String positionName,
            String phone,
            String email,
            String fullName);

    com.kopi.kopi.dto.EmployeeDetailDto getEmployeeDetail(Integer userId);

    void updateEmployee(Integer userId, com.kopi.kopi.dto.UpdateEmployeeRequest req);

    void banUser(Integer userId);

    void demoteEmployeeToCustomer(Integer userId);

    org.springframework.data.domain.Page<com.kopi.kopi.dto.CustomerListDto> listCustomers(int page, int size,
            String fullName,
            String phone,
            String email,
            String roleName);
}
