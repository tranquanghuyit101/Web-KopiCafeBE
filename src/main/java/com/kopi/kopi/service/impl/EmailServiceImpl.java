//package com.kopi.kopi.service.impl;
//
//import com.kopi.kopi.service.EmailService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.stereotype.Service;
//
//@Service
//public class EmailServiceImpl implements EmailService {
//    @Autowired private JavaMailSender mailSender;
//
//    @Override
//    public void send(String to, String subject, String text) {
//        SimpleMailMessage msg = new SimpleMailMessage();
//        msg.setFrom("duynhatvo05@gmail.com"); // khuyến nghị trùng spring.mail.username
//        msg.setTo(to);
//        msg.setSubject(subject);
//        msg.setText(text);
//        mailSender.send(msg);
//    }
//}

package com.kopi.kopi.service.impl;

import com.kopi.kopi.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    // Ưu tiên dùng app.mail.from; nếu không có thì fallback về spring.mail.username
    @Value("${app.mail.from:${spring.mail.username}}")
    private String from;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(@NonNull String to, @NonNull String subject, String content) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from); // phải trùng tài khoản gửi để tránh bị reject (đặc biệt với Gmail)
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(content == null ? "" : content);
        mailSender.send(msg);
    }
}

