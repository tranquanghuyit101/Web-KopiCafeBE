package com.kopi.kopi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

//  DTO verify OTP (OTP phải là 6 chữ số)
public record VerifyOtpRequest(

        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không hợp lệ")
        String email,

        @NotBlank(message = "OTP không được để trống")
        @Pattern(regexp = "^\\d{6}$", message = "OTP phải gồm 6 chữ số")
        String otp
) {}
