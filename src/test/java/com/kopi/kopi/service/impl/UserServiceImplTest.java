package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.AddressHome;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.entity.enums.UserStatus;
import com.kopi.kopi.repository.AddressHomeRepository;
import com.kopi.kopi.repository.PositionRepository;
import com.kopi.kopi.repository.RoleRepository;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.security.ForceChangeStore;
import com.kopi.kopi.service.EmailService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserServiceImpl using JUnit5 + Mockito.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private ForceChangeStore forceChangeStore;
    @Mock private AddressHomeRepository addressHomeRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PositionRepository positionRepository;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(
                userRepository,
                passwordEncoder,
                emailService,
                forceChangeStore,
                addressHomeRepository,
                roleRepository,
                positionRepository
        );
    }

    @AfterEach
    void tearDown() {
        // no-op for now, placeholder for future resources cleanup
    }

    // ---------------- resetPassword ----------------

    @Test
    void should_DoNothing_When_EmailNotFound() {
        // Given
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());

        // When
        service.resetPassword("a@b.com");

        // Then
        verify(userRepository, never()).save(any());
        verify(emailService, never()).send(anyString(), anyString(), anyString());
        verify(forceChangeStore, never()).set(anyString(), anyBoolean());
    }

    @Test
    void should_SetTempPassword_SendMail_SetFlag_When_UserExists() {
        // Given
        User u = new User();
        u.setEmail("e@kopi.com");
        u.setFullName("Kopi User");
        when(userRepository.findByEmail("e@kopi.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");

        // When
        service.resetPassword("e@kopi.com");

        // Then
        verify(passwordEncoder).encode(argThat(pwd -> pwd != null && pwd.length() >= 8));
        verify(userRepository).save(argThat(saved -> "ENCODED".equals(saved.getPasswordHash())));
        verify(forceChangeStore).set("e@kopi.com", true);
        verify(emailService).send(eq("e@kopi.com"), contains("temporary password"), contains("Temporary password"));
    }

    @Test
    void should_StillProceed_When_EmailServiceThrows() {
        // Given
        User u = new User();
        u.setEmail("e@kopi.com");
        u.setFullName("Kopi User");
        when(userRepository.findByEmail("e@kopi.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");
        doThrow(new RuntimeException("SMTP down")).when(emailService)
                .send(anyString(), anyString(), anyString());

        // When
        service.resetPassword("e@kopi.com");

        // Then
        verify(userRepository).save(any(User.class));
        verify(forceChangeStore).set("e@kopi.com", true);
        // exception swallowed, so test completes successfully
    }

    @Test
    void should_EncodePassword_When_ResetPassword() {
        // Given
        User u = new User();
        u.setEmail("e@kopi.com");
        when(userRepository.findByEmail("e@kopi.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");

        // When
        service.resetPassword("e@kopi.com");

        // Then
        verify(passwordEncoder).encode(anyString());
        verify(userRepository).save(argThat(saved -> "ENCODED".equals(saved.getPasswordHash())));
    }

    // ---------------- changePassword ----------------

    @Test
    void should_UpdatePasswordAndClearFlag_When_ChangePassword() {
        // Given
        User u = new User();
        u.setEmail("e@kopi.com");
        when(userRepository.findByEmail("e@kopi.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.encode("newpass")).thenReturn("HASHED");

        // When
        service.changePassword("e@kopi.com", "newpass");

        // Then
        verify(passwordEncoder).encode("newpass");
        verify(userRepository).save(argThat(saved -> "HASHED".equals(saved.getPasswordHash())));
        verify(forceChangeStore).set("e@kopi.com", false);
    }

    @Test
    void should_Throw_When_UserNotFound_OnChangePassword() {
        // Given
        when(userRepository.findByEmail("no@kopi.com")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.changePassword("no@kopi.com", "x"))
                .isInstanceOf(NoSuchElementException.class);
    }

    // ---------------- mustChangePassword / clearForceChangePassword ----------------

    @Test
    void should_ReturnFlag_FromStore() {
        // Given
        when(forceChangeStore.get("u@kopi.com")).thenReturn(true);

        // When
        boolean need = service.mustChangePassword("u@kopi.com");

        // Then
        assertThat(need).isTrue();
    }

    @Test
    void should_ClearFlag_InStore() {
        // Given + When
        service.clearForceChangePassword("u@kopi.com");

        // Then
        verify(forceChangeStore).set("u@kopi.com", false);
    }

    // ---------------- listEmployees ----------------

    @Test
    void should_MapEmployees_ToEmployeeSimpleDto() {
        // Given
        User u1 = new User();
        u1.setUserId(1);
        u1.setFullName("A");
        u1.setStatus(UserStatus.ACTIVE);

        User u2 = new User();
        u2.setUserId(2);
        u2.setFullName("B");
        // position null, status null

        when(userRepository.findByRoleRoleIdAndStatusNot(eq(2), eq(UserStatus.BANNED)))
                .thenReturn(List.of(u1, u2));

        // When
        var list = service.listEmployees();

        // Then
        assertThat(list).hasSize(2);
        assertThat(list.get(0).userId()).isEqualTo(1);
        assertThat(list.get(0).fullName()).isEqualTo("A");
        assertThat(list.get(0).status()).isEqualTo("ACTIVE");
        assertThat(list.get(1).positionName()).isNull();
        assertThat(list.get(1).status()).isNull();
    }

    // ---------------- searchEmployees ----------------

    @Test
    void should_SearchAndMapEmployees_WithFilters() {
        // Given
        // Spec is built inside service; we just return what a correct repo would return for that Spec
        User u = new User();
        u.setUserId(10);
        u.setFullName("Nguyen Van A");
        u.setEmail("a@kopi.com");
        u.setPhone("090");
        u.setStatus(UserStatus.ACTIVE);

        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(u));

        // When
        var list = service.searchEmployees("DEV", "090", "a@kopi.com", "nguyen");

        // Then
        assertThat(list).hasSize(1);
        assertThat(list.get(0).userId()).isEqualTo(10);
        assertThat(list.get(0).status()).isEqualTo("ACTIVE");
    }

    // ---------------- getEmployeeDetail ----------------

    @Test
    void should_ReturnDetail_WithLatestAddress_When_Present() {
        // Given
        User u = new User();
        u.setUserId(5);
        u.setUsername("usr");
        u.setEmail("u@kopi.com");
        u.setPhone("123");
        when(userRepository.findById(5)).thenReturn(Optional.of(u));

        AddressHome ah = new AddressHome();
        ah.setStreet("1st");
        ah.setCity("HCM");
        ah.setDistrict("1");
        when(addressHomeRepository.findTopByUserUserIdOrderByCreatedAtDesc(5)).thenReturn(Optional.of(ah));

        // When
        var dto = service.getEmployeeDetail(5);

        // Then
        assertThat(dto.userId()).isEqualTo(5);
        assertThat(dto.street()).isEqualTo("1st");
        assertThat(dto.city()).isEqualTo("HCM");
        assertThat(dto.district()).isEqualTo("1");
    }

    @Test
    void should_ReturnDetail_WithNullAddress_When_NotPresent() {
        // Given
        User u = new User();
        u.setUserId(6);
        when(userRepository.findById(6)).thenReturn(Optional.of(u));
        when(addressHomeRepository.findTopByUserUserIdOrderByCreatedAtDesc(6)).thenReturn(Optional.empty());

        // When
        var dto = service.getEmployeeDetail(6);

        // Then
        assertThat(dto.street()).isNull();
        assertThat(dto.city()).isNull();
        assertThat(dto.district()).isNull();
    }

    // ---------------- updateEmployee ----------------

    @Test
    void should_AllowRoleChange_FromCustomerToEmployee() {
        // Given
        User u = new User();
        u.setUserId(7);
        var roleCustomer = new com.kopi.kopi.entity.Role();
        roleCustomer.setRoleId(3);
        u.setRole(roleCustomer);
        when(userRepository.findById(7)).thenReturn(Optional.of(u));

        var roleEmployee = new com.kopi.kopi.entity.Role();
        roleEmployee.setRoleId(2);
        when(roleRepository.findById(2)).thenReturn(Optional.of(roleEmployee));

        var req = new com.kopi.kopi.dto.UpdateEmployeeRequest(2, null, null);

        // When
        service.updateEmployee(7, req);

        // Then
        assertThat(u.getRole().getRoleId()).isEqualTo(2);
        verify(userRepository).save(u);
    }

    @Test
    void should_RejectRoleChange_When_NotAllowed() {
        // Given
        User u = new User();
        u.setUserId(8);
        var roleEmployee = new com.kopi.kopi.entity.Role();
        roleEmployee.setRoleId(2);
        u.setRole(roleEmployee);
        when(userRepository.findById(8)).thenReturn(Optional.of(u));

        var roleAdmin = new com.kopi.kopi.entity.Role();
        roleAdmin.setRoleId(1);
        when(roleRepository.findById(1)).thenReturn(Optional.of(roleAdmin));

        var req = new com.kopi.kopi.dto.UpdateEmployeeRequest(1, null, null);

        // When / Then
        assertThatThrownBy(() -> service.updateEmployee(8, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role change not allowed");
        verify(userRepository, never()).save(any());
    }

    @Test
    void should_UpdatePosition_When_PositionProvided() {
        // Given
        User u = new User();
        u.setUserId(9);
        when(userRepository.findById(9)).thenReturn(Optional.of(u));

        var pos = new com.kopi.kopi.entity.Position();
        pos.setPositionId(100);
        pos.setPositionName("Dev");
        when(positionRepository.findById(100)).thenReturn(Optional.of(pos));

        var req = new com.kopi.kopi.dto.UpdateEmployeeRequest(null, 100, null);

        // When
        service.updateEmployee(9, req);

        // Then
        assertThat(u.getPosition()).isNotNull();
        assertThat(u.getPosition().getPositionId()).isEqualTo(100);
        verify(userRepository).save(u);
    }

    @Test
    void should_SetStatus_When_Valid_AndIgnoreInvalid() {
        // Given
        User u = new User();
        u.setUserId(11);
        when(userRepository.findById(11)).thenReturn(Optional.of(u));

        // valid status
        var reqValid = new com.kopi.kopi.dto.UpdateEmployeeRequest(null, null, "active");
        service.updateEmployee(11, reqValid);
        assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);

        // invalid status (should be ignored)
        var before = u.getStatus();
        var reqInvalid = new com.kopi.kopi.dto.UpdateEmployeeRequest(null, null, "___invalid___");
        service.updateEmployee(11, reqInvalid);
        assertThat(u.getStatus()).isEqualTo(before);

        verify(userRepository, times(2)).save(u);
    }

    // ---------------- banUser ----------------

    @Test
    void should_BanUser() {
        // Given
        User u = new User();
        u.setUserId(12);
        when(userRepository.findById(12)).thenReturn(Optional.of(u));

        // When
        service.banUser(12);

        // Then
        assertThat(u.getStatus()).isEqualTo(UserStatus.BANNED);
        verify(userRepository).save(u);
    }

    // ---------------- demoteEmployeeToCustomer ----------------

    @Test
    void should_DemoteEmployeeToCustomer_When_CustomerRoleExists() {
        // Given
        User u = new User();
        u.setUserId(13);
        u.setPosition(new com.kopi.kopi.entity.Position());
        when(userRepository.findById(13)).thenReturn(Optional.of(u));

        var customerRole = new com.kopi.kopi.entity.Role();
        customerRole.setRoleId(3);
        when(roleRepository.findById(3)).thenReturn(Optional.of(customerRole));

        // When
        service.demoteEmployeeToCustomer(13);

        // Then
        assertThat(u.getRole().getRoleId()).isEqualTo(3);
        assertThat(u.getPosition()).isNull();
        verify(userRepository).save(u);
    }

    @Test
    void should_Throw_When_CustomerRoleMissing() {
        // Given
        User u = new User();
        u.setUserId(14);
        when(userRepository.findById(14)).thenReturn(Optional.of(u));
        when(roleRepository.findById(3)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.demoteEmployeeToCustomer(14))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Customer role (id=3) not found");
        verify(userRepository, never()).save(any());
    }

    // ---------------- listCustomers ----------------

    @Test
    void should_ListCustomers_WithPaging_And_MapDto() {
        // Given
        User u1 = new User();
        u1.setUserId(1);
        u1.setFullName("A");
        u1.setStatus(UserStatus.ACTIVE);
        var role = new com.kopi.kopi.entity.Role();
        role.setName("CUSTOMER");
        u1.setRole(role);

        var page = new PageImpl<>(List.of(u1), PageRequest.of(0, 10), 1);
        when(userRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        // When
        var result = service.listCustomers(0, 10, "A", null, null, "cust");

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        var dto = result.getContent().get(0);
        assertThat(dto.userId()).isEqualTo(1);
        assertThat(dto.fullName()).isEqualTo("A");
        assertThat(dto.status()).isEqualTo("ACTIVE");
        assertThat(dto.roleName()).isEqualTo("CUSTOMER");
    }
}
