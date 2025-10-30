package com.kopi.kopi.service;

import com.kopi.kopi.entity.NotificationTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface INotificationService {

    void notifyUsers(String type, String title, String body,
                     Map<String, Object> data, List<Integer> userIds, boolean alsoEmail);

    void onOrderPaid(Long orderId, Integer customerId, Integer staffId, String orderCode);
    void onOrderCompleted(Long orderId, Integer customerId, String orderCode);

    void markRead(Integer userId, Long targetId);
    void markAllRead(Integer userId);

    long getUnreadCount(Integer userId);
    Page<NotificationTarget> getNotifications(Integer userId, Pageable pageable);
}
