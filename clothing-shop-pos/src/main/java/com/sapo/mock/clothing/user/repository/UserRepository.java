package com.sapo.mock.clothing.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.sapo.mock.clothing.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
    User findByUsername(String username);
    boolean existsByUsername(String username);
    User findByRefreshTokenAndUsername(String refreshToken, String username);
}
