package com.kopi.kopi.repository;

import com.kopi.kopi.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {

    // DAILY
    @Query(value = """
  SELECT CAST(p.paid_at AS date) AS bucket_start,
         SUM(p.amount) AS total_sum,
         COUNT(DISTINCT p.order_id) AS order_count
  FROM dbo.payments p
  WHERE p.status = 'paid' AND p.paid_at BETWEEN :from AND :to
  GROUP BY CAST(p.paid_at AS date)
  ORDER BY bucket_start
  """, nativeQuery = true)
    List<Object[]> sumPaidByDay(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // WEEKLY
    @Query(value = """
  SELECT CAST(DATEADD(WEEK, DATEDIFF(WEEK, 0, p.paid_at), 0) AS date) AS bucket_start,
         SUM(p.amount) AS total_sum,
         COUNT(DISTINCT p.order_id) AS order_count
  FROM dbo.payments p
  WHERE p.status = 'paid' AND p.paid_at BETWEEN :from AND :to
  GROUP BY CAST(DATEADD(WEEK, DATEDIFF(WEEK, 0, p.paid_at), 0) AS date)
  ORDER BY bucket_start
  """, nativeQuery = true)
    List<Object[]> sumPaidByWeek(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // MONTHLY
    @Query(value = """
  SELECT DATEFROMPARTS(YEAR(p.paid_at), MONTH(p.paid_at), 1) AS bucket_start,
         SUM(p.amount) AS total_sum,
         COUNT(DISTINCT p.order_id) AS order_count
  FROM dbo.payments p
  WHERE p.status = 'paid' AND p.paid_at BETWEEN :from AND :to
  GROUP BY DATEFROMPARTS(YEAR(p.paid_at), MONTH(p.paid_at), 1)
  ORDER BY bucket_start
  """, nativeQuery = true)
    List<Object[]> sumPaidByMonth(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // QUARTERLY
    @Query(value = """
  SELECT DATEFROMPARTS(YEAR(p.paid_at), ((DATEPART(QUARTER, p.paid_at)-1)*3)+1, 1) AS bucket_start,
         SUM(p.amount) AS total_sum,
         COUNT(DISTINCT p.order_id) AS order_count
  FROM dbo.payments p
  WHERE p.status = 'paid' AND p.paid_at BETWEEN :from AND :to
  GROUP BY DATEFROMPARTS(YEAR(p.paid_at), ((DATEPART(QUARTER, p.paid_at)-1)*3)+1, 1)
  ORDER BY bucket_start
  """, nativeQuery = true)
    List<Object[]> sumPaidByQuarter(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // YEARLY
    @Query(value = """
  SELECT DATEFROMPARTS(YEAR(p.paid_at), 1, 1) AS bucket_start,
         SUM(p.amount) AS total_sum,
         COUNT(DISTINCT p.order_id) AS order_count
  FROM dbo.payments p
  WHERE p.status = 'paid' AND p.paid_at BETWEEN :from AND :to
  GROUP BY DATEFROMPARTS(YEAR(p.paid_at), 1, 1)
  ORDER BY bucket_start
  """, nativeQuery = true)
    List<Object[]> sumPaidByYear(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
  SELECT COALESCE(SUM(p.amount), 0)
  FROM dbo.payments p
  WHERE p.status='paid' AND p.paid_at BETWEEN :from AND :to
  """, nativeQuery = true)
    java.math.BigDecimal sumPaidBetween(@Param("from") java.time.LocalDateTime from,
                                        @Param("to")   java.time.LocalDateTime to);

    @Query(value = """
  SELECT COUNT(1)
  FROM dbo.payments p
  WHERE p.status='paid' AND p.paid_at BETWEEN :from AND :to
  """, nativeQuery = true)
    int countPaidBetween(@Param("from") java.time.LocalDateTime from,
                         @Param("to")   java.time.LocalDateTime to);

    @Query(value = """
  SELECT COUNT(1)
  FROM dbo.payments p
  WHERE p.status='pending'
  """, nativeQuery = true)
    int countPendingPayments();

}
