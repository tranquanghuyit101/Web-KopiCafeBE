package com.kopi.kopi.service;

import com.kopi.kopi.dto.RegisterRequest;

public interface IAuthService {
    void registerOrResend(RegisterRequest req);
    void verifyOtp(String email, String otp);
    void markVerifiedByGoogle(String email);
}
