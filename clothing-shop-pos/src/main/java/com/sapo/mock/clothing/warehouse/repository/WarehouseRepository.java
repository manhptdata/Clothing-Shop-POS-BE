package com.sapo.mock.clothing.warehouse.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sapo.mock.clothing.entity.Warehouse;

@Repository
public interface warehouseRepository extends JpaRepository<Warehouse, Integer> {

	List<Warehouse> findByActiveTrue();

}
