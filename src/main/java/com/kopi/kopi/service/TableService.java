package com.kopi.kopi.service;

public interface TableService {
    void setOccupiedIfHasPendingOrders(Integer tableId);
    void setAvailableIfNoPendingOrders(Integer tableId);
}


