package com.sapo.mock.clothing.warehouse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sapo.mock.clothing.entity.WarehouseStock;

@Repository
public interface warehouseStockRepository extends JpaRepository<WarehouseStock, Integer> {

}
