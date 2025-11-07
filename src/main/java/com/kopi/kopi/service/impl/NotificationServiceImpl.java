package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.*;
import com.kopi.kopi.entity.enums.UserStatus;
import com.kopi.kopi.repository.NotificationRepository;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.service.EmailService;
import com.kopi.kopi.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    
    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
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
        String title = "Cập nhật trạng thái đơn hàng #" + order.getOrderCode();
        String message = String.format(
            "Đơn hàng của bạn (Mã: %s) đã được cập nhật trạng thái từ '%s' sang '%s'. %s",
            order.getOrderCode(),
            getStatusDisplayName(previousStatus),
            getStatusDisplayName(newStatus),
            statusMessage
        );
        
        // Lưu thông báo vào database
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
        
        // Gửi email thông báo
        try {
            emailService.send(
                customer.getEmail(),
                title,
                message + "\n\nCảm ơn bạn đã sử dụng dịch vụ của chúng tôi!"
            );
        } catch (Exception ex) {
            logger.warn("Failed to send email notification to customer {}: {}", 
                customer.getEmail(), ex.getMessage());
        }
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
            : "Khách hàng";
        String orderInfo = String.format(
            "Đơn hàng #%s của %s (ID: %d)",
            order.getOrderCode(),
            customerName,
            order.getOrderId()
        );
        
        String title = "Cập nhật trạng thái đơn hàng";
        String message = String.format(
            "%s đã thay đổi trạng thái từ '%s' sang '%s'.",
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
            
            // Lưu thông báo vào database
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
            
            // Gửi email thông báo
            try {
                emailService.send(
                    staff.getEmail(),
                    title,
                    message + "\n\nVui lòng kiểm tra hệ thống để xem chi tiết."
                );
            } catch (Exception ex) {
                logger.warn("Failed to send email notification to staff {}: {}", 
                    staff.getEmail(), ex.getMessage());
            }
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
        
        // Gửi email nếu có email
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                emailService.send(user.getEmail(), title, message);
            } catch (Exception ex) {
                logger.warn("Failed to send email notification to user {}: {}", 
                    user.getEmail(), ex.getMessage());
            }
        }
    }
    
    private String getStatusDisplayName(String status) {
        if (status == null) return "Không xác định";
        return switch (status.toUpperCase()) {
            case "PENDING" -> "Đang chờ";
            case "ACCEPTED" -> "Đã chấp nhận";
            case "REJECTED" -> "Đã từ chối";
            case "READY" -> "Sẵn sàng";
            case "SHIPPING" -> "Đang giao hàng";
            case "COMPLETED" -> "Hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            case "PAID" -> "Đã thanh toán";
            default -> status;
        };
    }
    
    private String getStatusMessage(String status) {
        if (status == null) return "";
        return switch (status.toUpperCase()) {
            case "ACCEPTED" -> "Đơn hàng của bạn đã được chấp nhận và đang được xử lý.";
            case "REJECTED" -> "Rất tiếc, đơn hàng của bạn đã bị từ chối. Vui lòng liên hệ với chúng tôi để biết thêm chi tiết.";
            case "READY" -> "Đơn hàng của bạn đã sẵn sàng để nhận.";
            case "SHIPPING" -> "Đơn hàng của bạn đang được giao đến bạn.";
            case "COMPLETED" -> "Đơn hàng của bạn đã hoàn thành. Cảm ơn bạn đã sử dụng dịch vụ!";
            case "CANCELLED" -> "Đơn hàng của bạn đã bị hủy.";
            default -> "";
        };
    }
}

