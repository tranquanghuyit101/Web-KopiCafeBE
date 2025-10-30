package com.kopi.kopi.repository;

import com.kopi.kopi.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepo extends JpaRepository<Notification, Long> {}
