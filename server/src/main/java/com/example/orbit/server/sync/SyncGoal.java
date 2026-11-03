package com.example.orbit.server.sync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
    name = "sync_goals",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sync_goals_user_client",
        columnNames = {"user_id", "client_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class SyncGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rowId;

    @Column(nullable = false)
    private long userId;

    @Column(name = "client_id", nullable = false)
    private long clientId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 24)
    private String tone;

    @Column(nullable = false, length = 24)
    private String status;

    private Long targetDate;

    @Column(nullable = false)
    private long createdAt;

    @Column(nullable = false)
    private long updatedAt;

    private Long completedAt;

    @Column(nullable = false)
    private int sortOrder;
}
