package com.kopi.kopi.service.impl;

import com.kopi.kopi.dto.RegisterRequest;
import com.kopi.kopi.entity.Role;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.entity.UserOtp;
import com.kopi.kopi.entity.enums.UserStatus;
import com.kopi.kopi.repository.RoleRepository;
import com.kopi.kopi.repository.UserOtpRepository;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.service.EmailService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepo;
    @Mock
    private UserOtpRepository otpRepo;
    @Mock
    private RoleRepository roleRepo;
    @Mock
    private EmailService emailService;

    private AuthServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AuthServiceImpl(userRepo, otpRepo, roleRepo, emailService);
        // set TTL OTP để test ổn định
        ReflectionTestUtils.setField(service, "otpTtlSeconds", 60);
    }

    @AfterEach
    void tearDown() {
    }

    // ===== Helpers =====
    private Role customerRole() {
        Role r = new Role();
        r.setRoleId(1);
        r.setName("CUSTOMER");
        return r;
    }

    private User user(Integer id, String email, boolean verified, UserStatus status) {
        User u = new User();
        u.setUserId(id);
        u.setEmail(email);
        u.setUsername("u" + id);
        u.setFullName("u" + id);
        u.setEmailVerified(verified);
        u.setStatus(status);
        u.setPasswordHash("$2a$hash");
        return u;
    }

    // ================= registerOrResend =================

    @Test
    void should_RegisterNewUser_CreateOtp_AndSendEmail() {
        // Given
        RegisterRequest req = new RegisterRequest("new@kopi.com", "newuser", "secret");
        when(userRepo.findByEmailIgnoreCase("new@kopi.com")).thenReturn(Optional.empty());
        when(userRepo.findByUsername("newuser")).thenReturn(Optional.empty());
        when(roleRepo.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole()));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setUserId(100);
            return u;
        });
        when(otpRepo.save(any(UserOtp.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        service.registerOrResend(req);

        // Then
        ArgumentCaptor<User> userCap = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(userCap.capture());
        User saved = userCap.getValue();
        assertThat(saved.getEmail()).isEqualTo("new@kopi.com");
        assertThat(saved.getUsername()).isEqualTo("newuser");
        assertThat(saved.getRole().getName()).isEqualTo("CUSTOMER");
        assertThat(saved.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(saved.isEmailVerified()).isFalse();

        verify(otpRepo).save(any(UserOtp.class));
        verify(emailService).send(eq("new@kopi.com"), anyString(), contains("Mã OTP"));
    }

    @Test
    void should_ResendOtp_ForExistingUnverifiedUser_UpdateUsernameAndPassword_AndRoleIfNull() {
        // Given
        User existing = user(10, "exist@kopi.com", false, UserStatus.INACTIVE);
        existing.setUsername("oldname");
        existing.setRole(null);
        when(userRepo.findByEmailIgnoreCase("exist@kopi.com")).thenReturn(Optional.of(existing));
        when(userRepo.findByUsername("newname")).thenReturn(Optional.empty());
        when(roleRepo.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole()));

        RegisterRequest req = new RegisterRequest("exist@kopi.com", "newname", "newpass");

        // When
        service.registerOrResend(req);

        // Then
        assertThat(existing.getUsername()).isEqualTo("newname");
        assertThat(existing.getRole()).isNotNull();
        assertThat(existing.getStatus()).isEqualTo(UserStatus.INACTIVE);

        verify(otpRepo).deleteByUserUserId(10);
        verify(otpRepo).save(any(UserOtp.class));
        verify(emailService).send(eq("exist@kopi.com"), anyString(), contains("Mã OTP"));
        // the service updates fields but does not explicitly save the user in this flow
    }

    @Test
    void should_Throw_When_EmailAlreadyVerified() {
        // Given
        User verified = user(1, "a@b.com", true, UserStatus.ACTIVE);
        when(userRepo.findByEmailIgnoreCase("a@b.com")).thenReturn(Optional.of(verified));

        // When / Then
        RegisterRequest req = new RegisterRequest("a@b.com", "name", "pass");
        assertThatThrownBy(() -> service.registerOrResend(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Email đã được sử dùng");
        verifyNoInteractions(otpRepo, emailService);
    }

    @Test
    void should_Throw_When_UsernameAlreadyTaken_OnNewRegister() {
        // Given
        when(userRepo.findByEmailIgnoreCase("x@x.com")).thenReturn(Optional.empty());
        when(userRepo.findByUsername("dup")).thenReturn(Optional.of(new User()));

        // When / Then
        RegisterRequest req = new RegisterRequest("x@x.com", "dup", "pw");
        assertThatThrownBy(() -> service.registerOrResend(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tên tài khoản đã tồn tại");
        verifyNoInteractions(otpRepo, emailService);
    }

    // ================= verifyOtp =================

    @Test
    void should_VerifyOtp_Success_AndActivateUser_DeleteOtps() {
        // Given
        User u = user(5, "u@kopi.com", false, UserStatus.INACTIVE);
        when(userRepo.findByEmailIgnoreCase("u@kopi.com")).thenReturn(Optional.of(u));

        // mock 1 OTP hợp lệ (chưa hết hạn, hash match)
        String plainOtp = "123456";
        var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        UserOtp otp = new UserOtp();
        otp.setUser(u);
        otp.setOtpHash(encoder.encode(plainOtp));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        when(otpRepo.findByUserUserId(5)).thenReturn(List.of(otp));

        // When
        service.verifyOtp("u@kopi.com", "123456");

        // Then
        assertThat(u.isEmailVerified()).isTrue();
        assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepo).save(u);
        verify(otpRepo).deleteByUserUserId(5);
    }

    @Test
    void should_Throw_When_OtpWrongOrExpired() {
        // Given
        User u = user(6, "u2@kopi.com", false, UserStatus.INACTIVE);
        when(userRepo.findByEmailIgnoreCase("u2@kopi.com")).thenReturn(Optional.of(u));

        // OTP sai hoặc hết hạn
        UserOtp expired = new UserOtp();
        expired.setUser(u);
        expired.setOtpHash(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("999999"));
        expired.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(otpRepo.findByUserUserId(6)).thenReturn(List.of(expired));

        // When / Then
        assertThatThrownBy(() -> service.verifyOtp("u2@kopi.com", "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("OTP sai hoặc đã hết hạn");
        verify(userRepo, never()).save(any());
        verify(otpRepo, never()).deleteByUserUserId(anyInt());
    }

    @Test
    void should_ReturnEarly_When_UserAlreadyVerified() {
        // Given
        User u = user(7, "v@kopi.com", true, UserStatus.ACTIVE);
        when(userRepo.findByEmailIgnoreCase("v@kopi.com")).thenReturn(Optional.of(u));

        // When
        service.verifyOtp("v@kopi.com", "whatever");

        // Then
        verify(userRepo, never()).save(any());
        verify(otpRepo, never()).findByUserUserId(anyInt());
        verify(otpRepo, never()).deleteByUserUserId(anyInt());
    }

    @Test
    void should_Throw_When_UserNotFound_OnVerifyOtp() {
        // Given
        when(userRepo.findByEmailIgnoreCase("no@kopi.com")).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> service.verifyOtp("no@kopi.com", "123456"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Không tìm thấy user");
    }

    // ================= markVerifiedByGoogle =================

    @Test
    void should_MarkVerifiedAndActivate_When_UserNotBanned() {
        // Given
        User u = user(8, "g@kopi.com", false, UserStatus.INACTIVE);
        when(userRepo.findByEmailIgnoreCase("g@kopi.com")).thenReturn(Optional.of(u));

        // When
        service.markVerifiedByGoogle("g@kopi.com");

        // Then
        assertThat(u.isEmailVerified()).isTrue();
        assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(userRepo).save(u);
        verify(otpRepo).deleteByUserUserId(8);
    }

    @Test
    void should_MarkVerifiedButKeepBanned_When_UserBanned() {
        // Given
        User u = user(9, "b@kopi.com", false, UserStatus.BANNED);
        when(userRepo.findByEmailIgnoreCase("b@kopi.com")).thenReturn(Optional.of(u));

        // When
        service.markVerifiedByGoogle("b@kopi.com");

        // Then
        assertThat(u.isEmailVerified()).isTrue();
        assertThat(u.getStatus()).isEqualTo(UserStatus.BANNED);
        verify(userRepo).save(u);
        verify(otpRepo).deleteByUserUserId(9);
    }

    @Test
    void should_DoNothing_When_UserNotFound_OnGoogleVerify() {
        // Given
        when(userRepo.findByEmailIgnoreCase("none@kopi.com")).thenReturn(Optional.empty());

        // When
        service.markVerifiedByGoogle("none@kopi.com");

        // Then
        verify(userRepo, never()).save(any());
        verify(otpRepo, never()).deleteByUserUserId(anyInt());
    }
}
