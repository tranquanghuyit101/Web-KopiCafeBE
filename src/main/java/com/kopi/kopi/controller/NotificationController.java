package com.kopi.kopi.controller;

import com.kopi.kopi.config.SseHub;
import com.kopi.kopi.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final INotificationService service;
    private final SseHub hub;

    // Nếu p.getName() không phải userId, đổi cách lấy userId từ JWT/session
    private Integer uid(Principal p) { return Integer.valueOf(p.getName()); }

    @GetMapping("/unread-count")
    public Map<String, Long> unreadCount(Principal p) {
        return Map.of("count", service.getUnreadCount(uid(p)));
    }

    @GetMapping
    public Object list(Principal p,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "20") int size) {
        return service.getNotifications(uid(p), PageRequest.of(page, size));
    }

    @PatchMapping("/{targetId}/read")
    public void markRead(Principal p, @PathVariable Long targetId) {
        service.markRead(uid(p), targetId);
    }

    @PostMapping("/read-all")
    public void readAll(Principal p) { service.markAllRead(uid(p)); }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(Principal p) { return hub.subscribe(uid(p)); }
}
