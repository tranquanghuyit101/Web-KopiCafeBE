package com.kopi.kopi.payload.response;

public class LoginResponse {
    private String token;
    private boolean forceChangePassword;

    public LoginResponse(String token, boolean forceChangePassword) {
        this.token = token;
        this.forceChangePassword = forceChangePassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isForceChangePassword() {
        return forceChangePassword;
    }

    public void setForceChangePassword(boolean forceChangePassword) {
        this.forceChangePassword = forceChangePassword;
    }
}

