package com.sapo.mock.clothing.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.sapo.mock.clothing.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {
	User findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsByPhone(String phone);

	User findByRefreshTokenAndUsername(String refreshToken, String username);

	List<User> findByActiveTrue();

	List<User> findAll();

	Optional<User> findById(Integer id);
}
