package com.sapo.mock.clothing.invoice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.sapo.mock.clothing.entity.InvoiceItem;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Integer>, JpaSpecificationExecutor<InvoiceItem> {
    java.util.List<InvoiceItem> findByInvoiceId(Integer invoiceId);

}
