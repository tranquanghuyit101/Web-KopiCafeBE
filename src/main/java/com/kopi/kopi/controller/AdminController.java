package com.kopi.kopi.controller;

import com.kopi.kopi.dto.EmployeeSimpleDto;
import com.kopi.kopi.dto.PositionDto;
import com.kopi.kopi.dto.UpdateEmployeeRequest;
import com.kopi.kopi.service.IUserService;
import com.kopi.kopi.service.PositionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/apiv1/admin")
public class AdminController {

    private final IUserService userService;
    private final PositionService positionService;
    private final com.kopi.kopi.repository.RoleRepository roleRepository;

    public AdminController(IUserService userService, PositionService positionService,
            com.kopi.kopi.repository.RoleRepository roleRepository) {
        this.userService = userService;
        this.positionService = positionService;
        this.roleRepository = roleRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeSimpleDto>> listEmployees(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String positionName,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String phone,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String email,
            @org.springframework.web.bind.annotation.RequestParam(name = "fullName", required = false) String fullName) {
        var list = userService.searchEmployees(positionName, phone, email, fullName);
        return ResponseEntity.ok(list);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/employees/{id}")
    public ResponseEntity<com.kopi.kopi.dto.EmployeeDetailDto> getEmployee(
            @org.springframework.web.bind.annotation.PathVariable Integer id) {
        var dto = userService.getEmployeeDetail(id);
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/positions")
    public ResponseEntity<java.util.List<PositionDto>> listPositions() {
        return ResponseEntity.ok(positionService.listPositions());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/roles")
    public ResponseEntity<java.util.List<com.kopi.kopi.dto.RoleDto>> listRoles() {
        var roles = roleRepository.findAll();
        var dtos = roles.stream().map(r -> new com.kopi.kopi.dto.RoleDto(r.getRoleId(), r.getName())).toList();
        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.web.bind.annotation.PutMapping("/employees/{id}")
    public ResponseEntity<Void> updateEmployee(
            @org.springframework.web.bind.annotation.PathVariable Integer id,
            @org.springframework.web.bind.annotation.RequestBody UpdateEmployeeRequest req) {
        userService.updateEmployee(id, req);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.web.bind.annotation.DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> demoteEmployee(@org.springframework.web.bind.annotation.PathVariable Integer id) {
        userService.demoteEmployeeToCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/customers")
    public ResponseEntity<org.springframework.data.domain.Page<com.kopi.kopi.dto.CustomerListDto>> listCustomers(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "15") int size,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String fullName,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String phone,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String email,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String roleName) {
        return ResponseEntity.ok(userService.listCustomers(page, size, fullName, phone, email, roleName));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/customers/{id}")
    public ResponseEntity<com.kopi.kopi.dto.EmployeeDetailDto> getCustomerDetail(
            @org.springframework.web.bind.annotation.PathVariable Integer id) {
        var dto = userService.getEmployeeDetail(id);
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.web.bind.annotation.PutMapping("/customers/{id}")
    public ResponseEntity<Void> updateCustomer(
            @org.springframework.web.bind.annotation.PathVariable Integer id,
            @org.springframework.web.bind.annotation.RequestBody UpdateEmployeeRequest req) {
        // reuse existing updateEmployee logic which supports roleId and status updates
        userService.updateEmployee(id, req);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.web.bind.annotation.DeleteMapping("/customers/{id}")
    public ResponseEntity<Void> banCustomer(@org.springframework.web.bind.annotation.PathVariable Integer id) {
        userService.banUser(id);
        return ResponseEntity.noContent().build();
    }
}