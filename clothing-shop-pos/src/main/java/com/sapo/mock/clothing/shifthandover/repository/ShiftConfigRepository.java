package com.sapo.mock.clothing.shifthandover.repository;

import com.sapo.mock.clothing.entity.ShiftConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftConfigRepository extends JpaRepository<ShiftConfig, Integer> {
    List<ShiftConfig> findByActiveTrue();
}
