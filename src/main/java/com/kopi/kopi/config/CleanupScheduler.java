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

    // khoảng thời gian xoá user chưa xác thực (mặc định 1 phút: PT1M trong
    // properties)
    @Value("${app.auth.cleanup.unverified.duration:PT1M}")
    private Duration unverifiedLifetime;

    public CleanupScheduler(UserOtpRepository otpRepo, UserRepository userRepo) {
        this.otpRepo = otpRepo;
        this.userRepo = userRepo;
    }

    /**
     *  Chạy mỗi 300s: dọn OTP đã hết hạn.
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
     *  Chạy mỗi 300000s cho chất: xoá user chưa verify quá hạn (email_verified=false &
     * created_at < now - duration).
     */
    @Transactional
    @Scheduled(fixedDelayString = "${app.auth.cleanup.unverified.fixed-delay-ms:300000000}")
    public void cleanupUnverifiedUsers() {
        LocalDateTime threshold = LocalDateTime.now().minus(unverifiedLifetime);
        userRepo.deleteByEmailVerifiedIsFalseAndCreatedAtBefore(threshold);
        // System.out.println("Deleted unverified users: " + deleted);
    }
}
