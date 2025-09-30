package com.kopi.kopi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kopi.kopi.dto.JwtLoginRequest;
import com.kopi.kopi.security.UserPrincipal;
import com.kopi.kopi.security.JwtTokenProvider;

// ADD
import com.kopi.kopi.service.IUserService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/apiv1/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    // ADD
    private final IUserService userService;

    // THÊM userService vào constructor (không xoá tham số cũ)
    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          IUserService userService) { // ADD
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService; // ADD
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody JwtLoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
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

            boolean forceChange = userService.mustChangePassword(principal.getUser().getEmail());
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("forceChangePassword", forceChange);
            return ResponseEntity.ok(Map.of("data", data));


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




}
