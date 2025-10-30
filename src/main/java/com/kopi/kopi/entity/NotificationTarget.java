package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "notification_targets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationTarget {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(nullable = false)
    private Integer userId;                 // staff hoặc customer

    @Column(nullable = false, length = 20)
    private String channel;                 // inapp | email

    @Column(nullable = false, length = 20)
    private String deliveryStatus = "pending"; // pending/sent/failed

    private Instant deliveredAt;
    private Instant readAt;
}
