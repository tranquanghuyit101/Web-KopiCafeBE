package com.kopi.kopi.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tables", schema = "dbo")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiningTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "table_id")
    private Integer tableId;

    @Column(name = "number", nullable = false)
    private Integer number;

    @Column(name = "name")
    private String name;

    @Column(name = "status", nullable = false, length = 20)
    private String status; // AVAILABLE | OCCUPIED | DISABLED | RESERVED

    @Column(name = "qr_token", nullable = false, length = 64)
    private String qrToken;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}


