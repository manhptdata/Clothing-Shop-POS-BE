package com.sapo.mock.clothing.customer.repository;

import com.sapo.mock.clothing.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer>, JpaSpecificationExecutor<Customer> {
    // Search ACTIVE customers by keyword (case-insensitive).
    @Query("SELECT c FROM Customer c WHERE c.status = 'ACTIVE' AND " +
            "(LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR c.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<Customer> searchActiveCustomers(@Param("keyword") String keyword, Pageable pageable);


    // Used for automatic phone number duplicate validation.
    boolean existsByPhone(String phone);

    // Check if the phone number is used by another customer (excluding the current ID).
    boolean existsByPhoneAndIdNot(String phone, Integer id);

    // detail customer by id, only if ACTIVE
    @Query("SELECT c FROM Customer c WHERE c.customerGroup.id = :groupId AND c.status = com.sapo.mock.clothing.util.constant.CustomerStatusEnum.ACTIVE")
    Page<Customer> findCustomersByGroupId(@Param("groupId") Integer groupId, Pageable pageable);
}
