package com.kopi.kopi.repository;

import com.kopi.kopi.entity.DiscountEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface DiscountEventRepository extends JpaRepository<DiscountEvent, Integer> {
	@Query("""
		select ev from DiscountEvent ev
		join ev.products dep
		where dep.product.productId = :productId
		  and ev.active = true
		  and ev.startsAt <= :now and ev.endsAt >= :now
		""")
	Optional<DiscountEvent> findActiveEventByProductId(@Param("productId") Integer productId, @Param("now") LocalDateTime now);
}


