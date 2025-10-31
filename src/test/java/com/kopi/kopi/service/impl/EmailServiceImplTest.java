package com.kopi.kopi.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailServiceImpl svc;

    @BeforeEach
    void setUp() throws Exception {
        svc = new EmailServiceImpl(mailSender);
        var f = EmailServiceImpl.class.getDeclaredField("from");
        f.setAccessible(true);
        f.set(svc, "noreply@kopi.test");
    }

    @Test
    void send_should_call_mailSender_with_expected_values() {
        svc.send("to@kopi.test", "Hello", "body text");

        ArgumentCaptor<SimpleMailMessage> cap = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(cap.capture());
        SimpleMailMessage msg = cap.getValue();
        assertThat(msg.getTo()).containsExactly("to@kopi.test");
        assertThat(msg.getSubject()).isEqualTo("Hello");
        assertThat(msg.getText()).isEqualTo("body text");
        assertThat(msg.getFrom()).isEqualTo("noreply@kopi.test");
    }

    @Test
    void send_null_content_should_send_empty_text() {
        svc.send("to2@kopi.test", "Sub", null);

        ArgumentCaptor<SimpleMailMessage> cap = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(cap.capture());
        SimpleMailMessage msg = cap.getValue();
        assertThat(msg.getText()).isEqualTo("");
    }
}
