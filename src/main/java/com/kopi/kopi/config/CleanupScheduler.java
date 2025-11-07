package com.kopi.kopi.config;

import com.kopi.kopi.repository.UserOtpRepository;
import com.kopi.kopi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class CleanupScheduler {

    private final UserOtpRepository otpRepo;
    private final UserRepository userRepo;
    private final com.kopi.kopi.service.UnverifiedUserDeletionService deletionService;

    // khoảng thời gian xoá user chưa xác thực (mặc định 1 phút: PT1M trong
    // properties)
    @Value("${app.auth.cleanup.unverified.duration:PT1M}")
    private Duration unverifiedLifetime;

    public CleanupScheduler(UserOtpRepository otpRepo, UserRepository userRepo,
            com.kopi.kopi.service.UnverifiedUserDeletionService deletionService) {
        this.otpRepo = otpRepo;
        this.userRepo = userRepo;
        this.deletionService = deletionService;
    }

    /**
     * Chạy mỗi 300s: dọn OTP đã hết hạn.
     */
    @Transactional
    @Scheduled(fixedDelayString = "${app.auth.cleanup.otp.fixed-delay-ms:300000}")
    public void cleanupExpiredOtps() {
        LocalDateTime now = LocalDateTime.now();
        otpRepo.deleteByExpiresAtBefore(now);
        // có thể log nếu cần
        // System.out.println("Deleted expired OTPs: " + deleted);
    }

    /**
     * Chạy mỗi 300000s cho chất: xoá user chưa verify quá hạn (email_verified=false
     * &
     * created_at < now - duration).
     */
    @Transactional
    @Scheduled(fixedDelayString = "${app.auth.cleanup.unverified.fixed-delay-ms:300000000}")
    public void cleanupUnverifiedUsers() {
        LocalDateTime threshold = LocalDateTime.now().minus(unverifiedLifetime);
        // Retrieve candidates and attempt to delete them individually. This avoids
        // a single bulk delete failing due to FK constraints (e.g.
        // positions.created_by_user_id)
        java.util.List<com.kopi.kopi.entity.User> candidates = userRepo
                .findByEmailVerifiedIsFalseAndCreatedAtBefore(threshold);
        for (com.kopi.kopi.entity.User u : candidates) {
            try {
                deletionService.deleteUserById(u.getUserId());
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                // Skip users that cannot be deleted due to FK constraints and continue
                System.err.println("Skipping delete for user " + u.getUserId() + ": " + ex.getMessage());
            }
        }
        // Optionally log how many were deleted (not tracked here)
    }
}
