
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
import com.kopi.kopi.service.IAuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepo;
    private final UserOtpRepository otpRepo;
    private final RoleRepository roleRepo;
    private final EmailService emailService;

    private final BCryptPasswordEncoder bCrypt = new BCryptPasswordEncoder();
    private final Random random = new Random();

    @Value("${app.auth.otp.ttl-seconds:30}")
    private int otpTtlSeconds;

    public AuthServiceImpl(UserRepository userRepo,
            UserOtpRepository otpRepo,
            RoleRepository roleRepo,
            EmailService emailService) {
        this.userRepo = userRepo;
        this.otpRepo = otpRepo;
        this.roleRepo = roleRepo;
        this.emailService = emailService;
    }

    private String genOtp6() {
        return String.valueOf(100000 + random.nextInt(900000));
    }

    private Role resolveCustomerRole() {
        Optional<Role> byName = roleRepo.findByName("CUSTOMER");
        if (byName.isPresent())
            return byName.get();
        return roleRepo.findById(1).orElseThrow(
                () -> new IllegalStateException("Role CUSTOMER không tồn tại. Hãy seed bảng roles trước."));
    }

    @Override
    @Transactional
    public void registerOrResend(RegisterRequest req) {
        String email = req.email() == null ? null : req.email().trim();
        String username = req.username() == null ? null : req.username().trim();

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không được để trống nha");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Tên đăng nhập không được để trống nha");
        }
        if (req.password() == null || req.password().isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }

        LocalDateTime now = LocalDateTime.now();

        var userOpt = userRepo.findByEmailIgnoreCase(email);
        if (userOpt.isPresent()) {
            var user = userOpt.get();
            if (user.isEmailVerified()) {
                throw new IllegalStateException("Email đã được sử dùng");
            }
            // Nếu username mới khác username cũ, kiểm tra trùng

            if (username != null && !username.isBlank() && !username.equals(user.getUsername())) {
                var byUsername = userRepo.findByUsername(username);
                if (byUsername.isPresent() && !byUsername.get().getUserId().equals(user.getUserId())) {
                    throw new IllegalArgumentException("Tên tài khoản đã tồn tại vui lòng nhập tên khác");
                }
                user.setUsername(username);
            }
            user.setFullName(username); // 🟨 full_name = username để tránh NULL
            user.setPasswordHash(bCrypt.encode(req.password()));
            user.setStatus(UserStatus.INACTIVE);
            if (user.getRole() == null) {
                user.setRole(resolveCustomerRole());
            }
            user.setUpdatedAt(now);

            otpRepo.deleteByUserUserId(user.getUserId());
            createAndSendOtp(user, now);
            return;
        }

        // Trước khi tạo user mới, kiểm tra username đã tồn tại hay chưa
        var existingByUsername = userRepo.findByUsername(username);
        if (existingByUsername.isPresent()) {
            throw new IllegalArgumentException("Tên tài khoản đã tồn tại");
        }

        var user = new User();
        user.setEmail(email);
        user.setUsername(username);
        user.setFullName(username); // 🟨 tránh NULL cho full_name
        user.setPasswordHash(bCrypt.encode(req.password()));
        user.setRole(resolveCustomerRole()); // role_id=1 (CUSTOMER)
        user.setStatus(UserStatus.INACTIVE);
        user.setEmailVerified(false);
        user.setPhone(null); // cột phone NOT NULL => để rỗng
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userRepo.save(user);
        createAndSendOtp(user, now);
    }

    private void createAndSendOtp(User user, LocalDateTime now) {
        String otpPlain = genOtp6();

        var otp = new UserOtp();
        otp.setUser(user);
        otp.setOtpHash(bCrypt.encode(otpPlain));
        otp.setExpiresAt(now.plusSeconds(otpTtlSeconds));
        otpRepo.save(otp);

        String subject = "OTP xác thực tài khoản";
        String content = """
                Mã OTP của bạn: %s
                Hiệu lực: %d giây.
                """.formatted(otpPlain, otpTtlSeconds);

        try {
            // 🟨 FIX THỨ TỰ THAM SỐ: to, subject, body
            emailService.send(user.getEmail(), subject, content);
        } catch (Exception ex) {
            // Không để lỗi gửi mail làm vỡ flow đăng ký; log cảnh báo để dev kiểm tra
            org.slf4j.LoggerFactory.getLogger(getClass())
                    .warn("Gửi email OTP thất bại tới {}: {}", user.getEmail(), ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void verifyOtp(String email, String plainOtp) {
        var user = userRepo.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user"));

        if (user.isEmailVerified())
            return;

        var now = LocalDateTime.now();
        var list = otpRepo.findByUserUserId(user.getUserId());

        boolean ok = list.stream()
                .filter(o -> o.getExpiresAt().isAfter(now))
                .anyMatch(o -> bCrypt.matches(plainOtp, o.getOtpHash()));

        if (!ok) {
            throw new IllegalArgumentException("OTP sai hoặc đã hết hạn");
        }

        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        user.setUpdatedAt(now);
        userRepo.save(user);

        otpRepo.deleteByUserUserId(user.getUserId());
    }

    @Override
    @Transactional
    public void markVerifiedByGoogle(String email) {
        userRepo.findByEmailIgnoreCase(email).ifPresent(u -> {
            u.setEmailVerified(true);
            if (u.getStatus() != UserStatus.BANNED)
                u.setStatus(UserStatus.ACTIVE);
            u.setUpdatedAt(LocalDateTime.now());
            userRepo.save(u);
            otpRepo.deleteByUserUserId(u.getUserId());
        });
    }
}
