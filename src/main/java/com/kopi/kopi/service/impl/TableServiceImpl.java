package com.kopi.kopi.service.impl;

import com.kopi.kopi.entity.DiningTable;
import com.kopi.kopi.repository.DiningTableRepository;
import com.kopi.kopi.repository.OrderRepository;
import com.kopi.kopi.service.TableService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TableServiceImpl implements TableService {
    private final DiningTableRepository diningTableRepository;
    private final OrderRepository orderRepository;

    public TableServiceImpl(DiningTableRepository diningTableRepository, OrderRepository orderRepository) {
        this.diningTableRepository = diningTableRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public void setOccupiedIfHasPendingOrders(Integer tableId) {
        long pending = orderRepository.countByTable_TableIdAndStatus(tableId, "PENDING");
        diningTableRepository.findById(tableId).ifPresent(t -> {
            String newStatus = pending > 0 ? "OCCUPIED" : "AVAILABLE";
            if (!newStatus.equals(t.getStatus())) {
                t.setStatus(newStatus);
                t.setUpdatedAt(LocalDateTime.now());
                diningTableRepository.save(t);
            }
        });
    }

    @Override
    @Transactional
    public void setAvailableIfNoPendingOrders(Integer tableId) {
        long pending = orderRepository.countByTable_TableIdAndStatus(tableId, "PENDING");
        if (pending == 0) {
            diningTableRepository.findById(tableId).ifPresent(t -> {
                if (!"AVAILABLE".equals(t.getStatus())) {
                    t.setStatus("AVAILABLE");
                    t.setUpdatedAt(LocalDateTime.now());
                    diningTableRepository.save(t);
                }
            });
        }
    }
}


