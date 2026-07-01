package com.sapo.mock.clothing.shifthandover.repository;

import com.sapo.mock.clothing.entity.ShiftHandover;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShiftHandoverRepository extends JpaRepository<ShiftHandover, Integer> {
    List<ShiftHandover> findAllByOrderByCreatedAtDesc();
}
