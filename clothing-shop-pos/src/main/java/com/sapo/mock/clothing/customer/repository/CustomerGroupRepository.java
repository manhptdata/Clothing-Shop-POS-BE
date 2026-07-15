package com.sapo.mock.clothing.customer.repository;


import com.sapo.mock.clothing.customer.dto.response.CustomerGroupResponse;
import com.sapo.mock.clothing.entity.Customer;
import com.sapo.mock.clothing.entity.CustomerGroup;
import com.sapo.mock.clothing.util.constant.CustomerStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerGroupRepository extends JpaRepository<CustomerGroup, Integer> {


    /**
     * Lấy tất cả nhóm khách hàng đang hoạt động (Có phân trang và đếm số thành viên)
     */
    @Query("SELECT new com.sapo.mock.clothing.customer.dto.response.CustomerGroupResponse(" +
            "g.id, g.name, g.description, g.status, COUNT(c), g.note, g.minSpending, g.code, g.createdAt, bv.id, bv.name) " +
            "FROM CustomerGroup g " +
            "LEFT JOIN Customer c ON c.customerGroup.id = g.id " +
            "LEFT JOIN g.birthdayVoucher bv " +
            "WHERE g.status = com.sapo.mock.clothing.util.constant.CustomerStatusEnum.ACTIVE " +
            "GROUP BY g.id, g.name, g.description, g.status, g.note, g.minSpending, g.code, g.createdAt, bv.id, bv.name")
    Page<CustomerGroupResponse> findAllActiveGroups(Pageable pageable);


    /**
     * Tìm kiếm phân trang danh sách nhóm khách hàng theo từ khóa keyword
     */
    @Query("SELECT new com.sapo.mock.clothing.customer.dto.response.CustomerGroupResponse(" +
            "g.id, g.name, g.description, g.status, COUNT(c), g.note, g.minSpending, g.code, g.createdAt, bv.id, bv.name) " +
            "FROM CustomerGroup g " +
            "LEFT JOIN Customer c ON c.customerGroup.id = g.id " +
            "LEFT JOIN g.birthdayVoucher bv " +
            "WHERE (:keyword IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND g.status = com.sapo.mock.clothing.util.constant.CustomerStatusEnum.ACTIVE " +
            "GROUP BY g.id, g.name, g.description, g.status, g.note, g.minSpending, g.code, g.createdAt, bv.id, bv.name")
    Page<CustomerGroupResponse> searchGroups(@Param("keyword") String keyword, Pageable pageable);

    // Lấy chi tiết 1 nhóm kèm theo số lượng khách
    @Query("SELECT new com.sapo.mock.clothing.customer.dto.response.CustomerGroupResponse(" +
            "g.id, g.name, g.description, g.status, COUNT(c), g.note, g.minSpending, g.code, g.createdAt, bv.id, bv.name) " +
            "FROM CustomerGroup g " +
            "LEFT JOIN Customer c ON c.customerGroup.id = g.id " +
            "LEFT JOIN g.birthdayVoucher bv " +
            "WHERE g.id = :id " +
            "GROUP BY g.id, g.name, g.description, g.status, g.note, g.minSpending, g.code, g.createdAt, bv.id, bv.name")
    Optional<CustomerGroupResponse> findGroupDetailById(@Param("id") Integer id);

    /**
     * Quét tìm hạng phù hợp: minSpending <= totalSpent < maxSpending
     */
    @Query("SELECT cg FROM CustomerGroup cg " +
            "WHERE cg.status = :status " +
            "AND cg.minSpending IS NOT NULL " +
            "AND :totalSpent >= cg.minSpending " +
            "ORDER BY cg.minSpending DESC")
    List<CustomerGroup> findSuitableGroup(
            @Param("totalSpent") BigDecimal totalSpent,
            @Param("status") CustomerStatusEnum status
    );

    // Lấy tất cả các hạng thành viên đang hoạt động (Đồng, Bạc, Vàng) để đối chiếu
    List<CustomerGroup> findByStatus(CustomerStatusEnum status);



}