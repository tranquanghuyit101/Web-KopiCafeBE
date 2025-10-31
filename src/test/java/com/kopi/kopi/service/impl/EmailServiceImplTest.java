package com.kopi.kopi.service.impl;

import com.kopi.kopi.service.EmailService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmailServiceImpl(mailSender);
        // giả lập inject value từ @Value
        service.from = "testsender@example.com";
    }

    @AfterEach
    void tearDown() {
        // no-op
    }

    @Test
    void should_SendEmail_WithValidFields() {
        // Given
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // When
        service.send("user@example.com", "Hello", "Hi there");

        // Then
        verify(mailSender, times(1)).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertThat(msg.getFrom()).isEqualTo("testsender@example.com");
        assertThat(msg.getTo()).containsExactly("user@example.com");
        assertThat(msg.getSubject()).isEqualTo("Hello");
        assertThat(msg.getText()).isEqualTo("Hi there");
    }

    @Test
    void should_UseEmptyBody_When_ContentNull() {
        // Given
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // When
        service.send("user@example.com", "NoBody", null);

        // Then
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).isEqualTo("");
    }

    @Test
    void should_Throw_When_ToIsNull() {
        // Expect
        assertThatThrownBy(() -> service.send(null, "Subject", "Body"))
                .isInstanceOf(NullPointerException.class);
        verify(mailSender, never()).send(any());
    }

    @Test
    void should_Throw_When_SubjectIsNull() {
        // Expect
        assertThatThrownBy(() -> service.send("user@example.com", null, "Body"))
                .isInstanceOf(NullPointerException.class);
        verify(mailSender, never()).send(any());
    }
}
