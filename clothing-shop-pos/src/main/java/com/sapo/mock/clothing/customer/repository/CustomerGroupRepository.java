package com.sapo.mock.clothing.customer.repository;


import com.sapo.mock.clothing.customer.dto.response.CustomerGroupResponse;
import com.sapo.mock.clothing.entity.CustomerGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerGroupRepository extends JpaRepository<CustomerGroup, Integer> {

    // Retrieve all customer groups.
    Page<CustomerGroup> findAllByOrderByIdAsc(Pageable pageable);

   /* JPQL ==> totalCustomers là dữ liệu dẫn xuất (derived data) — đã có thể tính được từ dữ liệu có sẵn → không cần lưu riêng,
    tránh rủi ro không đồng bộ.*/
    //  tìm kiếm nhóm ACTIVE
    @Query("SELECT new com.sapo.mock.clothing.customer.dto.response.CustomerGroupResponse(" +
            "g.id, g.name, g.description, g.status, COUNT(c), g.note, g.createdAt) " +
            "FROM CustomerGroup g " +
            "LEFT JOIN Customer c ON c.customerGroup.id = g.id " +
            "WHERE g.status = 'ACTIVE' AND LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "GROUP BY g.id, g.name, g.description, g.status, g.note, g.createdAt")
    Page<CustomerGroupResponse> searchGroups(@Param("keyword") String keyword, Pageable pageable);

    // Lấy chi tiết 1 nhóm kèm theo số lượng khách
    @Query("SELECT new com.sapo.mock.clothing.customer.dto.response.CustomerGroupResponse(" +
            "g.id, g.name, g.description, g.status, COUNT(c), g.note, g.createdAt) " +
            "FROM CustomerGroup g " +
            "LEFT JOIN Customer c ON c.customerGroup.id = g.id " +
            "WHERE g.id = :id GROUP BY g.id, g.name, g.description, g.status, g.note, g.createdAt")
    CustomerGroupResponse getGroupDetailById(@Param("id") Integer id);
}