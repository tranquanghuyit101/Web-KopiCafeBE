package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.*;
import com.kopi.kopi.entity.enums.UserStatus;
import com.kopi.kopi.repository.NotificationRepository;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.service.EmailService;
import com.kopi.kopi.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);
    
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private NotificationServiceImpl self; // Self-injection để @Async hoạt động
    
    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }
    
    // Self-injection để @Async method có thể được gọi từ trong class
    // Dùng @Lazy để tránh circular dependency
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    public void setSelf(NotificationServiceImpl self) {
        this.self = self;
    }
    
    @Override
    @Transactional
    public void notifyOrderStatusChangeToCustomer(OrderEntity order, String previousStatus, String newStatus) {
        if (order.getCustomer() == null) {
            logger.warn("Order {} has no customer, skipping customer notification", order.getOrderId());
            return;
        }
        
        User customer = order.getCustomer();
        String statusMessage = getStatusMessage(newStatus);
        String title = "Order Status Update #" + order.getOrderCode();
        String message = String.format(
            "Your order (Code: %s) has been updated from '%s' to '%s'. %s",
            order.getOrderCode(),
            getStatusDisplayName(previousStatus),
            getStatusDisplayName(newStatus),
            statusMessage
        );
        
        // Lưu thông báo vào database (đồng bộ - trong transaction)
        Notification notification = Notification.builder()
                .user(customer)
                .order(order)
                .title(title)
                .message(message)
                .type("ORDER_STATUS_CHANGE")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        
        // Gửi email thông báo bất đồng bộ (không chặn response)
        sendEmailSafely(
            customer.getEmail(),
            title,
            message + "\n\nThank you for using our service!"
        );
    }
    
    @Override
    @Transactional
    public void notifyOrderStatusChangeToStaff(OrderEntity order, String previousStatus, String newStatus) {
        // Lấy tất cả staff (ADMIN và STAFF roles)
        // findByRoleNameIn sẽ tự động tìm users có role.name trong danh sách
        List<User> staffUsers = userRepository.findByRoleNameIn(List.of("ADMIN", "STAFF"));
        
        if (staffUsers.isEmpty()) {
            logger.warn("No staff users found to notify");
            return;
        }
        
        String customerName = order.getCustomer() != null 
            ? order.getCustomer().getFullName() 
            : "Customer";
        String orderInfo = String.format(
            "Order #%s of %s (ID: %d)",
            order.getOrderCode(),
            customerName,
            order.getOrderId()
        );
        
        String title = "Order Status Update";
        String message = String.format(
            "%s has changed status from '%s' to '%s'.",
            orderInfo,
            getStatusDisplayName(previousStatus),
            getStatusDisplayName(newStatus)
        );
        
        // Gửi thông báo cho từng staff
        for (User staff : staffUsers) {
            // Chỉ gửi cho staff đang active
            if (staff.getStatus() != UserStatus.ACTIVE) {
                continue;
            }
            
            // Lưu thông báo vào database (đồng bộ - trong transaction)
            Notification notification = Notification.builder()
                    .user(staff)
                    .order(order)
                    .title(title)
                    .message(message)
                    .type("ORDER_STATUS_CHANGE")
                    .isRead(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            notificationRepository.save(notification);
            
            // Gửi email thông báo bất đồng bộ (không chặn response)
            sendEmailSafely(
                staff.getEmail(),
                title,
                message + "\n\nPlease check the system for more details."
            );
        }
    }
    
    @Override
    @Transactional
    public void sendNotification(User user, String title, String message, String type, OrderEntity order) {
        Notification notification = Notification.builder()
                .user(user)
                .order(order)
                .title(title)
                .message(message)
                .type(type != null ? type : "SYSTEM")
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notification);
        
        // Gửi email nếu có email (bất đồng bộ)
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            sendEmailSafely(user.getEmail(), title, message);
        }
    }
    
    /**
     * Gửi email bất đồng bộ để không chặn response
     */
    @Async
    public void sendEmailAsync(String to, String subject, String content) {
        try {
            emailService.send(to, subject, content);
        } catch (Exception ex) {
            logger.warn("Failed to send email notification to {}: {}", 
                to, ex.getMessage());
        }
    }
    
    /**
     * Helper method để gọi sendEmailAsync an toàn (fallback nếu self chưa inject)
     */
    private void sendEmailSafely(String to, String subject, String content) {
        if (self != null) {
            self.sendEmailAsync(to, subject, content);
        } else {
            // Fallback: gọi trực tiếp nếu self chưa inject (không async nhưng không crash)
            logger.debug("Self not injected yet, sending email synchronously");
            try {
                emailService.send(to, subject, content);
            } catch (Exception ex) {
                logger.warn("Failed to send email notification to {}: {}", 
                    to, ex.getMessage());
            }
        }
    }
    
    private String getStatusDisplayName(String status) {
        if (status == null) return "Unknown";
        return switch (status.toUpperCase()) {
            case "PENDING" -> "Pending";
            case "ACCEPTED" -> "Accepted";
            case "REJECTED" -> "Rejected";
            case "READY" -> "Ready";
            case "SHIPPING" -> "Shipping";
            case "COMPLETED" -> "Completed";
            case "CANCELLED" -> "Cancelled";
            case "PAID" -> "Paid";
            default -> status;
        };
    }
    
    private String getStatusMessage(String status) {
        if (status == null) return "";
        return switch (status.toUpperCase()) {
            case "ACCEPTED" -> "Your order has been accepted and is being processed.";
            case "REJECTED" -> "Unfortunately, your order has been rejected. Please contact us for more details.";
            case "READY" -> "Your order is ready for pickup.";
            case "SHIPPING" -> "Your order is out for delivery.";
            case "COMPLETED" -> "Your order has been completed. Thank you for using our service!";
            case "CANCELLED" -> "Your order has been cancelled.";
            default -> "";
        };
    }
}

