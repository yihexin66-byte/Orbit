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
    name = "sync_tasks",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sync_tasks_user_client",
        columnNames = {"user_id", "client_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class SyncTask {

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
    private String priority;

    @Column(nullable = false, length = 24)
    private String category;

    private Long dueDate;

    @Column(nullable = false)
    private boolean isCompleted;

    @Column(nullable = false)
    private long createdAt;

    private Long completedAt;

    private Long goalClientId;

    private Long stageClientId;
}
