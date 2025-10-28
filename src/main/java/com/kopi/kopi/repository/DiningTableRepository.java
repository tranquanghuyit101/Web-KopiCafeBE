package com.kopi.kopi.repository;

import com.kopi.kopi.entity.DiningTable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiningTableRepository extends JpaRepository<DiningTable, Integer> {
    Optional<DiningTable> findByQrToken(String qrToken);
    Optional<DiningTable> findByNumber(Integer number);
    Page<DiningTable> findByStatus(String status, Pageable pageable);
}


