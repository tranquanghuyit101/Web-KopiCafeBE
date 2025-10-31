package com.kopi.kopi.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmtpEmailServiceTest {

    // 1. Mock dependency
    @Mock
    private JavaMailSender mailSender;

    // 2. Inject mock vào class cần test
    @InjectMocks
    private SmtpEmailService emailService;

    private final String MOCK_FROM_ADDRESS = "no-reply@kopi.com";

    @BeforeEach
    void setUp() {
        // 3. Gán giá trị cho trường @Value
        // Vì @Value không được xử lý bởi Mockito, chúng ta phải gán thủ công
        // giá trị 'from' cho emailService.
        ReflectionTestUtils.setField(emailService, "from", MOCK_FROM_ADDRESS);
    }

    @Test
    @DisplayName("send should_CallMailSender_When_ValidInputProvided")
    void should_CallMailSender_When_ValidInputProvided() {
        // Given
        String to = "recipient@example.com";
        String subject = "Test Mail";
        String content = "This is a test email.";

        // Tạo một ArgumentCaptor để bắt đối tượng SimpleMailMessage
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // When
        emailService.send(to, subject, content);

        // Then
        // 1. Xác minh mailSender.send() được gọi đúng 1 lần
        verify(mailSender, times(1)).send(messageCaptor.capture());

        // 2. Lấy message đã bị bắt
        SimpleMailMessage capturedMessage = messageCaptor.getValue();

        // 3. Khẳng định các trường của message là chính xác
        assertThat(capturedMessage.getFrom()).isEqualTo(MOCK_FROM_ADDRESS);
        assertThat(capturedMessage.getTo()).containsExactly(to);
        assertThat(capturedMessage.getSubject()).isEqualTo(subject);
        assertThat(capturedMessage.getText()).isEqualTo(content);
    }

    @Test
    @DisplayName("send should_SetEmptyString_When_ContentIsNull")
    void should_SetEmptyString_When_ContentIsNull() {
        // Given
        String to = "recipient@example.com";
        String subject = "Test Mail (Null Content)";
        String content = null; // Đây là điều kiện test

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        // When
        emailService.send(to, subject, content);

        // Then
        verify(mailSender, times(1)).send(messageCaptor.capture());
        SimpleMailMessage capturedMessage = messageCaptor.getValue();

        // Khẳng định quan trọng: text phải là chuỗi rỗng, không phải null
        assertThat(capturedMessage.getText()).isEqualTo("");
        assertThat(capturedMessage.getTo()).containsExactly(to);
        assertThat(capturedMessage.getSubject()).isEqualTo(subject);
        assertThat(capturedMessage.getFrom()).isEqualTo(MOCK_FROM_ADDRESS);
    }

    @Test
    @DisplayName("send should_PropagateMailException_When_SenderFails")
    void should_PropagateMailException_When_SenderFails() {
        // Given
        String to = "recipient@example.com";
        String subject = "Test Fail";
        String content = "This will fail.";

        // Cấu hình mock để ném ra ngoại lệ khi send() được gọi
        // Chúng ta tạo một lớp MailException ẩn danh vì nó là trừu tượng
        doThrow(new MailException("Simulated mail server error") {}).when(mailSender).send(any(SimpleMailMessage.class));

        // When / Then
        // Khẳng định rằng việc gọi emailService.send() sẽ ném ra MailException
        assertThatThrownBy(() -> emailService.send(to, subject, content))
                .isInstanceOf(MailException.class)
                .hasMessage("Simulated mail server error");

        // Xác minh rằng phương thức send của mailSender vẫn được gọi (mặc dù nó đã ném ra lỗi)
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}