package com.sapo.mock.clothing.receipt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sapo.mock.clothing.entity.StockLog;

@Repository
public interface StockLogRepository extends JpaRepository<StockLog, Integer> {
}