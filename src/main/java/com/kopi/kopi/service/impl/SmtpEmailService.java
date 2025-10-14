//package com.kopi.kopi.service.impl;
//
//import com.kopi.kopi.service.EmailService;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Primary;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.stereotype.Service;
//
//@Service
//@Primary // 🟨 THÊM DÒNG NÀY: đặt làm bean mặc định cho EmailService
//public class SmtpEmailService implements EmailService {
//
//    private final JavaMailSender mailSender;
//
//    // 🟨 From mặc định lấy từ app.mail.from, fallback sang spring.mail.username
//    @Value("${app.mail.from:${spring.mail.username}}")
//    private String from;
//
//    public SmtpEmailService(JavaMailSender mailSender) {
//        this.mailSender = mailSender;
//    }
//
//    @Override
//    public void send(String to, String subject, String content) {
//        SimpleMailMessage msg = new SimpleMailMessage();
//        msg.setFrom(from); // 🟨 Gmail yêu cầu From trùng tài khoản SMTP
//        msg.setTo(to);
//        msg.setSubject(subject);
//        msg.setText(content == null ? "" : content);
//        mailSender.send(msg);
//    }
//}
package com.kopi.kopi.service.impl;

import com.kopi.kopi.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Primary // 🟨 THÊM DÒNG NÀY: đặt làm bean mặc định cho EmailService
public class SmtpEmailService implements EmailService {

    private final JavaMailSender mailSender;

    // 🟨 From mặc định lấy từ app.mail.from, fallback sang spring.mail.username
    @Value("${app.mail.from:${spring.mail.username}}")
    private String from;

    public SmtpEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(String to, String subject, String content) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from); // 🟨 Gmail yêu cầu From trùng tài khoản SMTP
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(content == null ? "" : content);
        mailSender.send(msg);
    }
}
