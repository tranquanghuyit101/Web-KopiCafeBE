package com.kopi.kopi.repository;

import com.kopi.kopi.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    Page<Notification> findByUser_UserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
    
    List<Notification> findByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(Integer userId);
    
    long countByUser_UserIdAndIsReadFalse(Integer userId);
    
    @Query("SELECT n FROM Notification n WHERE n.user.userId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findAllByUserId(@Param("userId") Integer userId);
    
    // Fetch notifications with order and table eagerly to avoid lazy loading issues
    @Query("SELECT n FROM Notification n " +
           "LEFT JOIN FETCH n.order o " +
           "LEFT JOIN FETCH o.table t " +
           "LEFT JOIN FETCH o.address a " +
           "WHERE n.user.userId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findAllByUserIdWithOrder(@Param("userId") Integer userId);
}

