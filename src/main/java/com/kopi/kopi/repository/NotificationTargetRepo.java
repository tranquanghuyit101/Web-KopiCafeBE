package com.kopi.kopi.repository;

import com.kopi.kopi.entity.NotificationTarget;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface NotificationTargetRepo extends JpaRepository<NotificationTarget, Long> {

    @Query("""
    select count(nt) from NotificationTarget nt
    where nt.userId = :uid and nt.channel = 'inapp' and nt.readAt is null
  """)
    long countUnread(@Param("uid") Integer uid);

    @EntityGraph(attributePaths = "notification")
    @Query("""
    select nt from NotificationTarget nt
    where nt.userId = :uid and nt.channel = 'inapp'
    order by nt.notification.createdAt desc
  """)
    Page<NotificationTarget> pageForUser(@Param("uid") Integer uid, Pageable pageable);
}
