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
    name = "sync_growth_events",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sync_growth_user_client",
        columnNames = {"user_id", "client_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class SyncGrowthEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rowId;

    @Column(nullable = false)
    private long userId;

    @Column(name = "client_id", nullable = false)
    private long clientId;

    @Column(nullable = false, length = 32)
    private String type;

    @Column(nullable = false, length = 120)
    private String sourceKey;

    @Column(nullable = false)
    private int xp;

    @Column(nullable = false)
    private long occurredAt;

    @Column(nullable = false)
    private long epochDay;
}
