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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kopi.kopi.dto.JwtLoginRequest;
import com.kopi.kopi.security.UserPrincipal;
import com.kopi.kopi.security.JwtTokenProvider;


import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/apiv1/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody JwtLoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            User user = principal.getUser();
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

            //  xác định đường dẫn điều hướng theo vai trò
            String redirectPath = resolveRedirectPath(user);

            return ResponseEntity.ok(Map.of(
                    "data", Map.of("token", token), "redirectPath", redirectPath // trả về redirectPath để FE điều hướng
            ));
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

    @PostMapping("/apiv1/auth/google")
    public ResponseEntity<?> loginWithGoogleIdToken(@RequestBody Map<String, String> body) {
        String idToken = body.get("credential");
        if (idToken == null || idToken.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("msg", "Missing credential"));
        }
        //
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("msg","Implement verify + issue JWT"));
    }

}