package com.kopi.kopi.exception;

public class YouTubeQuotaExceededException extends RuntimeException {
    private final String resetTime;
    
    public YouTubeQuotaExceededException(String message, String resetTime) {
        super(message);
        this.resetTime = resetTime;
    }
    
    public String getResetTime() {
        return resetTime;
    }
}

