package com.sapo.mock.clothing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private String type; // e.g. LOW_STOCK, APPROVAL_REQUEST, SHIFT_HANDOVER, SYSTEM

    @Column(name = "target_role")
    private String targetRole; // e.g. ROLE_ADMIN, ROLE_WH

    @Column(name = "target_user_id")
    private Integer targetUserId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON payload for front-end actions

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
