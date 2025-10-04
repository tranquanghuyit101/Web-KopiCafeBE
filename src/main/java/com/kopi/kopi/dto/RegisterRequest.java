package com.kopi.kopi.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Đăng ký gồm: email + username + password */
public record RegisterRequest(

                @NotBlank(message = "Email không được để trống") @Email(message = "Email không hợp lệ") String email,

                @NotBlank(message = "Tên đăng nhập không được để trống") @Size(min = 3, max = 100, message = "Tên đăng nhập phải từ 3-100 ký tự") String username,

                @NotBlank(message = "Mật khẩu không được để trống") @Size(min = 8, max = 100, message = "Mật khẩu tối thiểu 8 ký tự") String password) {
}
