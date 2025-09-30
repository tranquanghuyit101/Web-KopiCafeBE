package com.kopi.kopi.service;

public interface EmailService {
    void send(String to, String subject, String text);
}
