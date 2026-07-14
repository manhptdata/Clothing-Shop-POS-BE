package com.sapo.mock.clothing.notification.repository;

import com.sapo.mock.clothing.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query("SELECT n FROM Notification n WHERE n.targetUserId = :userId ORDER BY n.createdAt DESC")
    List<Notification> findActiveNotificationsForUser(@Param("userId") Integer userId);
}
