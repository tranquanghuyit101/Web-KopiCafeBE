package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String type; // ORDER_PAID, ORDER_COMPLETED, ...

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String body;

    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String dataJson;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
