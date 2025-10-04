package com.kopi.kopi.repository;

import com.kopi.kopi.entity.UserOtp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserOtpRepository extends JpaRepository<UserOtp, Integer> {

    // Lấy tất cả OTP theo user (phục vụ verify và dọn dẹp)
    List<UserOtp> findByUserUserId(Integer userId);

    // Xoá toàn bộ OTP của 1 user (sau khi verify/resend)
    void deleteByUserUserId(Integer userId);

    // Dọn OTP hết hạn (cron gọi định kỳ)
    long deleteByExpiresAtBefore(LocalDateTime time);
}
