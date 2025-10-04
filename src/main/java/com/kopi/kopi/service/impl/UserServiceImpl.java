package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.User;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.security.ForceChangeStore;
import com.kopi.kopi.service.EmailService;
import com.kopi.kopi.service.IUserService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserServiceImpl implements IUserService {
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final ForceChangeStore forceChangeStore;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Qualifier("smtpEmailService") EmailService emailService, // match bean theo name
            ForceChangeStore forceChangeStore
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.forceChangeStore = forceChangeStore;
    }

    @Override
    @PreAuthorize("permitAll()")
    public void resetPassword(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) return;

        User user = userOpt.get();
        String tmp = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        user.setPasswordHash(passwordEncoder.encode(tmp));
        userRepository.save(user);

        // BẬT cờ trong file
        forceChangeStore.set(user.getEmail(), true);

        try {
            emailService.send(
                    user.getEmail(),
                    "[Kopi] Your temporary password",
                    "Hi " + user.getFullName() + ",\n\nYour temporary password is: " + tmp +
                            "\nPlease log in and change it immediately."
            );
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn("Send mail failed for {}: {}", user.getEmail(), ex.getMessage());
        }
        System.out.println("Temporary password for " + user.getEmail() + ": " + tmp);
    }

    @Override
    @PreAuthorize("permitAll()")
    public void changePassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email).orElseThrow();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // TẮT cờ trong file
        forceChangeStore.set(email, false);
    }

    @Override
    public boolean mustChangePassword(String email) {
        return forceChangeStore.get(email);
    }

    @Override
    public void clearForceChangePassword(String email) {
        forceChangeStore.set(email, false);
    }
}
