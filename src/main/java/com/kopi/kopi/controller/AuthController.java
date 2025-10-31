package com.kopi.kopi.controller;

import com.kopi.kopi.entity.Role;
import com.kopi.kopi.entity.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.kopi.kopi.payload.request.ForceChangePasswordRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.kopi.kopi.dto.JwtLoginRequest;
import com.kopi.kopi.security.UserPrincipal;
import com.kopi.kopi.security.JwtTokenProvider;

// ADD
import com.kopi.kopi.service.IUserService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDateTime;
import java.util.HashMap;

import java.util.Map;
import java.util.Optional;

// import DTO & Service cho OTP register/verify
import com.kopi.kopi.dto.ApiMessage;
import com.kopi.kopi.dto.RegisterRequest;
import com.kopi.kopi.dto.VerifyOtpRequest;
import jakarta.validation.Valid;
import com.kopi.kopi.service.IAuthService;

@RestController
@RequestMapping("/apiv1/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    // ADD
    private final IUserService userService;
    private final IAuthService authService;
    private final com.kopi.kopi.repository.UserRepository userRepository;

    // THÊM userService vào constructor (không xoá tham số cũ)
    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          IUserService userService,
                          IAuthService authService,
                          com.kopi.kopi.repository.UserRepository userRepository) { // ADD
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService; // ADD
        this.authService = authService; //
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody JwtLoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            User user = principal.getUser();

            // Update last login time for email/password login
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);
            int roleNumber = principal.getUser().getRole() != null && principal.getUser().getRole().getRoleId() != null
                    ? principal.getUser().getRole().getRoleId()
                    : 1;
            String token = jwtTokenProvider.generateToken(
                    principal.getUsername(),
                    roleNumber,
                    principal.getUser().getUserId(),
                    principal.getUser().getFullName(),
                    principal.getUser().getEmail(),
                    request.isRememberMe()
            );
          
          //merge duyPassword and DatRedirect, may have error
          boolean forceChange = userService.mustChangePassword(principal.getUser().getEmail());
          String redirectPath = resolveRedirectPath(user);

          Map<String, Object> data = new HashMap<>();
          data.put("token", token);
          data.put("forceChangePassword", forceChange);
          data.put("redirectPath", redirectPath);

          return ResponseEntity.ok(Map.of("data", data));
          //end merge
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Sai tài khoản hoặc mật khẩu");
        }
    }

    @DeleteMapping("/logout")
    public ResponseEntity<?> logout() {
        return ResponseEntity.ok(Map.of("message", "logged out"));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutPost() {
        return ResponseEntity.ok(Map.of("message", "logged out"));
    }

    @PreAuthorize("permitAll()")
    @PostMapping("/forgotPass")
    public ResponseEntity<?> forgotPassBody(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("msg", "Email is required"));
        }
        userService.resetPassword(email);
        return ResponseEntity.ok(Map.of(
                "data", true,
                "msg", "We sent a temporary password to your email"
        ));
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody com.kopi.kopi.payload.request.ChangePasswordRequest req) {
        // Lấy email từ user hiện tại (tránh đổi mật khẩu hộ tài khoản khác)
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof com.kopi.kopi.security.UserPrincipal up)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        String email = up.getUser().getEmail();

        userService.changePassword(email, req.getNewPassword());
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    private String resolveRedirectPath(User user) {
        String roleName = Optional.ofNullable(user.getRole())
                .map(Role::getName)
                .map(String::toUpperCase)
                .orElse("CUSTOMER");

        //  EMPLOYEE -> POS; ADMIN -> AdminDashboard; CUSTOMER -> Menu
        // Dữ liệu trong sql là dùng "STAFF" thay cho "EMPLOYEE" => map STAFF = EMPLOYEE
        return switch (roleName) {
            case "ADMIN" -> "/admin/dashboard"; // chưa có
            case "STAFF", "EMPLOYEE" -> "/employee/pos"; // chưa có
            default -> "/"; // home
        };
    }
    // ADD: Ép đổi mật khẩu khi user đang dùng mật khẩu tạm (mustChangePassword=true).
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/force-change-password")
    public ResponseEntity<?> forceChangePassword(
            @AuthenticationPrincipal com.kopi.kopi.security.UserPrincipal principal,
            @RequestBody ForceChangePasswordRequest req
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Unauthorized"));
        }
        if (req == null || req.getNew_password() == null || req.getNew_password().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 6 characters"));
        }

        String email = principal.getUser().getEmail();

        // Chỉ cho đổi khi đang bị ép đổi
        if (!userService.mustChangePassword(email)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Not required to force-change"));
        }

        userService.changePassword(email, req.getNew_password()); // change + clear flag ở service
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }


    @PostMapping("/apiv1/auth/google")
    public ResponseEntity<?> loginWithGoogleIdToken(@RequestBody Map<String, String> body) {
        String idToken = body.get("credential");
        if (idToken == null || idToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("msg", "Missing credential"));
        }
        //
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("msg","Implement verify + issue JWT"));
    }

    @PreAuthorize("permitAll()") // 🟨 cho phép gọi không cần auth
    @PostMapping("/register")    // full URL: /Kopi/apiv1/auth/register (vì có context-path /Kopi)
    public ResponseEntity<ApiMessage> register(@Valid @RequestBody RegisterRequest req) { // 🟨
        authService.registerOrResend(req);                                                // 🟨
        return ResponseEntity.ok(new ApiMessage("Đã gửi (hoặc gửi lại) OTP nếu email chưa kích hoạt")); // 🟨
    }

    @PreAuthorize("permitAll()") // 🟨
    @PostMapping("/verify-otp")   // full URL: /Kopi/apiv1/auth/verify-otp
    public ResponseEntity<ApiMessage> verify(@Valid @RequestBody VerifyOtpRequest req) { // 🟨
        authService.verifyOtp(req.email(), req.otp());                                    // 🟨
        return ResponseEntity.ok(new ApiMessage("Xác thực thành công"));                  // 🟨
    }
}
