package com.kopi.kopi.service;
import org.springframework.security.access.prepost.PreAuthorize;

public interface IUserService {
    @PreAuthorize("permitAll()")
    void resetPassword(String email);

    @PreAuthorize("permitAll()")
    void changePassword(String email, String newPassword);

    boolean mustChangePassword(String email);
    void clearForceChangePassword(String email);
}
