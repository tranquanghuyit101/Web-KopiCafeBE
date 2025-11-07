package com.kopi.kopi.service;

import com.kopi.kopi.entity.OrderEntity;
import com.kopi.kopi.entity.User;

public interface NotificationService {
    /**
     * Gửi thông báo cho customer khi trạng thái đơn hàng thay đổi
     */
    void notifyOrderStatusChangeToCustomer(OrderEntity order, String previousStatus, String newStatus);
    
    /**
     * Gửi thông báo cho tất cả staff khi trạng thái đơn hàng thay đổi
     */
    void notifyOrderStatusChangeToStaff(OrderEntity order, String previousStatus, String newStatus);
    
    /**
     * Gửi thông báo cho một user cụ thể
     */
    void sendNotification(User user, String title, String message, String type, OrderEntity order);
}

