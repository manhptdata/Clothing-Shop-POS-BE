package com.sapo.mock.clothing.returnorder.repository;

import com.sapo.mock.clothing.entity.ReturnOrderLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnOrderLineItemRepository extends JpaRepository<ReturnOrderLineItem, Integer> {

    List<ReturnOrderLineItem> findByReturnOrderId(Integer returnOrderId);
}
