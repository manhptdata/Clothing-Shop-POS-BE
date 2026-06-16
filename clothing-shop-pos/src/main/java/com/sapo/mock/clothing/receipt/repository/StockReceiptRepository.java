package com.sapo.mock.clothing.receipt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sapo.mock.clothing.entity.StockReceipt;

@Repository
public interface StockReceiptRepository extends JpaRepository<StockReceipt, Integer> {

}