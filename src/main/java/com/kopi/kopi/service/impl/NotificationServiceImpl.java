package com.kopi.kopi.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kopi.kopi.config.SseHub;
import com.kopi.kopi.entity.Notification;
import com.kopi.kopi.entity.NotificationTarget;
import com.kopi.kopi.entity.User;
import com.kopi.kopi.repository.NotificationRepo;
import com.kopi.kopi.repository.NotificationTargetRepo;
import com.kopi.kopi.repository.UserRepository;
import com.kopi.kopi.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepo nRepo;
    private final NotificationTargetRepo tRepo;
    private final SseHub hub;
    private final JavaMailSender mailSender;
    private final UserRepository userRepo;
    private final ObjectMapper om = new ObjectMapper();

    private String emailOf(Integer userId) {
        return userRepo.findById(userId).map(User::getEmail).orElse(null);
    }

    private String writeJson(Object o) {
        try { return om.writeValueAsString(o); }
        catch (Exception e) { return "{}"; }
    }

    @Transactional
    @Override
    public void notifyUsers(String type, String title, String body,
                            Map<String, Object> data, List<Integer> userIds, boolean alsoEmail) {
        var notif = nRepo.save(Notification.builder()
                .type(type).title(title).body(body)
                .dataJson(writeJson(data)).build());

        for (Integer uid : userIds) {
            // In-app
            var target = tRepo.save(NotificationTarget.builder()
                    .notification(notif).userId(uid).channel("inapp").build());

            // SSE realtime
            hub.push(uid, Map.of(
                    "targetId", target.getId(),
                    "id", notif.getId(),
                    "title", notif.getTitle(),
                    "body", notif.getBody(),
                    "type", notif.getType(),
                    "data", data,
                    "createdAt", notif.getCreatedAt().toString()
            ));

            // Email
            if (alsoEmail) {
                String email = emailOf(uid);
                try {
                    if (email == null || email.isBlank()) throw new IllegalStateException("no email");
                    SimpleMailMessage msg = new SimpleMailMessage();
                    msg.setTo(email);
                    msg.setSubject(title);
                    msg.setText(body + "\n\nDetails: " + notif.getDataJson());
                    mailSender.send(msg);

                    tRepo.save(NotificationTarget.builder()
                            .notification(notif).userId(uid).channel("email")
                            .deliveryStatus("sent").deliveredAt(Instant.now()).build());
                } catch (Exception ex) {
                    tRepo.save(NotificationTarget.builder()
                            .notification(notif).userId(uid).channel("email")
                            .deliveryStatus("failed").build());
                }
            }
        }
    }

    @Override
    public void onOrderPaid(Long orderId, Integer customerId, Integer staffId, String orderCode) {
        notifyUsers(
                "ORDER_PAID",
                "New paid order " + orderCode,
                "Customer has paid. Please prepare the order.",
                Map.of("orderId", orderId, "orderCode", orderCode),
                List.of(staffId),
                true   // email staff
        );
    }

    @Override
    public void onOrderCompleted(Long orderId, Integer customerId, String orderCode) {
        notifyUsers(
                "ORDER_COMPLETED",
                "Your order " + orderCode + " is ready",
                "Thanks for ordering. Your order has been completed.",
                Map.of("orderId", orderId, "orderCode", orderCode),
                List.of(customerId),
                true   // email customer
        );
    }

    // ===== Read/Unread =====
    @Transactional @Override
    public void markRead(Integer userId, Long targetId) {
        var nt = tRepo.findById(targetId).orElseThrow();
        if (!nt.getUserId().equals(userId) || !"inapp".equals(nt.getChannel())) return;
        if (nt.getReadAt() == null) { nt.setReadAt(Instant.now()); tRepo.save(nt); }
    }

    @Transactional @Override
    public void markAllRead(Integer userId) {
        var page = tRepo.pageForUser(userId, Pageable.ofSize(500));
        page.forEach(nt -> { if (nt.getReadAt()==null) { nt.setReadAt(Instant.now()); tRepo.save(nt);} });
    }

    @Override public long getUnreadCount(Integer userId) { return tRepo.countUnread(userId); }
    @Override public Page<NotificationTarget> getNotifications(Integer userId, Pageable pageable) {
        return tRepo.pageForUser(userId, pageable);
    }
}
