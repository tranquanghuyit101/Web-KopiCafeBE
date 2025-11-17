package com.kopi.kopi.controller;

import com.kopi.kopi.entity.Notification;
import com.kopi.kopi.repository.NotificationRepository;
import com.kopi.kopi.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/apiv1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    /**
     * Lấy danh sách thông báo của user hiện tại (có phân trang)
     * GET /apiv1/notifications?page=1&limit=50
     * Nếu không có page/limit thì trả về tất cả (dùng cho trang xem tất cả)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listNotifications(
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "limit", required = false) Integer limit
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Integer userId = principal.getUser().getUserId();
        String userRole = principal.getUser().getRole() != null 
            ? principal.getUser().getRole().getName() 
            : "CUSTOMER";

        List<Map<String, Object>> items;
        Map<String, Object> meta = new HashMap<>();

        // Nếu không có page/limit, trả về tất cả thông báo (dùng cho trang xem tất cả)
        if (page == null && limit == null) {
            List<Notification> allNotifications = notificationRepository.findAllByUserIdWithOrder(userId);
            // Log để debug
            org.slf4j.LoggerFactory.getLogger(getClass()).info(
                "Fetching all notifications for user {}: found {} notifications", 
                userId, allNotifications.size()
            );
            items = allNotifications.stream()
                    .map(n -> notificationToMap(n, userRole))
                    .collect(Collectors.toList());
            meta.put("totalItems", allNotifications.size());
            meta.put("currentPage", 1);
            meta.put("totalPage", 1);
            meta.put("prev", false);
            meta.put("next", false);
        } else {
            // Có phân trang
            int pageNum = Math.max(page != null ? page - 1 : 0, 0);
            int limitNum = Math.max(limit != null ? limit : 50, 1);
            Pageable pageable = PageRequest.of(pageNum, limitNum);
            Page<Notification> pageData = notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId, pageable);

            // Load order để tránh lazy loading
            List<Notification> notifications = pageData.getContent();
            notifications.forEach(n -> {
                if (n.getOrder() != null) {
                    // Trigger lazy load
                    n.getOrder().getOrderId();
                    if (n.getOrder().getTable() != null) {
                        n.getOrder().getTable().getTableId();
                    }
                    if (n.getOrder().getAddress() != null) {
                        n.getOrder().getAddress().getAddressId();
                    }
                }
            });

            items = notifications.stream()
                    .map(n -> notificationToMap(n, userRole))
                    .collect(Collectors.toList());

            meta.put("currentPage", pageData.getNumber() + 1);
            meta.put("totalPage", pageData.getTotalPages());
            meta.put("totalItems", pageData.getTotalElements());
            meta.put("prev", pageData.hasPrevious());
            meta.put("next", pageData.hasNext());
        }

        return ResponseEntity.ok(Map.of(
                "data", items,
                "meta", meta
        ));
    }

    /**
     * Lấy số lượng thông báo chưa đọc
     * GET /apiv1/notifications/unread-count
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();

        long unreadCount = notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }

    /**
     * Lấy danh sách thông báo chưa đọc
     * GET /apiv1/notifications/unread
     */
    @GetMapping("/unread")
    public ResponseEntity<Map<String, Object>> getUnreadNotifications() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        Integer userId = principal.getUser().getUserId();
        String userRole = principal.getUser().getRole() != null 
            ? principal.getUser().getRole().getName() 
            : "CUSTOMER";
        
        List<Notification> unreadNotifications = notificationRepository.findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> items = unreadNotifications.stream()
                .map(n -> notificationToMap(n, userRole))
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("data", items));
    }

    /**
     * Đánh dấu thông báo là đã đọc
     * PATCH /apiv1/notifications/{id}/read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markAsRead(@PathVariable("id") Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Kiểm tra xem thông báo có thuộc về user hiện tại không
        if (!notification.getUser().getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String userRole = principal.getUser().getRole() != null 
            ? principal.getUser().getRole().getName() 
            : "CUSTOMER";
        
        notification.setIsRead(true);
        notificationRepository.save(notification);

        return ResponseEntity.ok(Map.of("message", "OK", "data", notificationToMap(notification, userRole)));
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc
     * PATCH /apiv1/notifications/read-all
     */
    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllAsRead() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();

        List<Notification> unreadNotifications = notificationRepository.findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unreadNotifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unreadNotifications);

        return ResponseEntity.ok(Map.of("message", "OK", "count", unreadNotifications.size()));
    }

    /**
     * Xóa thông báo
     * DELETE /apiv1/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteNotification(@PathVariable("id") Integer id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Integer userId = ((UserPrincipal) auth.getPrincipal()).getUser().getUserId();

        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        // Kiểm tra xem thông báo có thuộc về user hiện tại không
        if (!notification.getUser().getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("message", "Forbidden"));
        }

        notificationRepository.delete(notification);
        return ResponseEntity.ok(Map.of("message", "OK"));
    }

    /**
     * Chuyển đổi Notification entity thành Map để trả về JSON
     * @param n Notification entity
     * @param userRole Role của user hiện tại (CUSTOMER, STAFF, ADMIN)
     */
    private Map<String, Object> notificationToMap(Notification n, String userRole) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", n.getNotificationId());
        map.put("title", n.getTitle());
        map.put("message", n.getMessage());
        map.put("type", n.getType());
        map.put("isRead", n.getIsRead());
        map.put("createdAt", n.getCreatedAt());
        
        // Thêm thông tin order nếu có
        if (n.getOrder() != null) {
            Map<String, Object> orderInfo = new HashMap<>();
            orderInfo.put("orderId", n.getOrder().getOrderId());
            orderInfo.put("orderCode", n.getOrder().getOrderCode());
            orderInfo.put("status", n.getOrder().getStatus());
            
            // Xác định loại đơn hàng: SHIPPING (có address) hoặc TABLE (có table)
            String orderType = null;
            if (n.getOrder().getAddress() != null) {
                orderType = "SHIPPING";
            } else if (n.getOrder().getTable() != null) {
                orderType = "TABLE";
                orderInfo.put("tableNumber", n.getOrder().getTable().getNumber());
            }
            orderInfo.put("orderType", orderType);
            
            // Thêm redirect URL dựa trên role và order type
            String redirectUrl = null;
            if ("CUSTOMER".equalsIgnoreCase(userRole)) {
                // Customer redirect về history
                redirectUrl = "/history";
            } else if (orderType != null) {
                // Staff redirect về shipping hoặc table order
                if ("SHIPPING".equals(orderType)) {
                    redirectUrl = "/shipping-order";
                } else if ("TABLE".equals(orderType)) {
                    redirectUrl = "/table-order";
                }
            }
            orderInfo.put("redirectUrl", redirectUrl);
            
            map.put("order", orderInfo);
        }
        
        return map;
    }
}

