package com.kopi.kopi.security;

import com.kopi.kopi.entity.Role;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.entity.enums.UserStatus;
import com.kopi.kopi.repository.RoleRepository;
import com.kopi.kopi.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements org.springframework.security.web.authentication.AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attrs = oauth2User.getAttributes();

        String email = (String) attrs.get("email");
        String name  = (String) attrs.getOrDefault("name", "");

        if (email == null || email.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Email not found in Google profile");
            return;
        }


        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User();
            u.setEmail(email);
            u.setUsername(email);
            u.setFullName(name);
            u.setPhone(null);
            u.setStatus(UserStatus.ACTIVE);
            u.setCreatedAt(LocalDateTime.now());
            u.setUpdatedAt(LocalDateTime.now());
            u.setLastLoginAt(null);
            u.setEmailVerified(true);
            u.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));


            Role role = roleRepository.findByName("CUSTOMER").orElse(null);
            if (role == null) {
                role = new Role();
                role.setName("CUSTOMER");
                role.setDescription("Customer created via Google Login");
                role = roleRepository.save(role);
            }
            u.setRole(role);
            return userRepository.save(u);
        });

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);


        int roleNumber = (user.getRole() != null && user.getRole().getRoleId() != null)
                ? user.getRole().getRoleId()
                : 1;

        boolean rememberMe = false;
        String token = jwtTokenProvider.generateToken(
                user.getEmail(),
                roleNumber,
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                rememberMe
        );

        String roleName = (user.getRole() != null && user.getRole().getName() != null)
                ? user.getRole().getName()
                : "CUSTOMER";

        String redirectUrl = UriComponentsBuilder.fromHttpUrl(frontendUrl)
                .path("/auth/login")
                .queryParam("token", token)
                .queryParam("role", roleName)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
