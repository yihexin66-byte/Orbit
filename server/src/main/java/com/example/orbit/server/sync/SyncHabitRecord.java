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
    name = "sync_habit_records",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sync_habit_records_user_client",
        columnNames = {"user_id", "client_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class SyncHabitRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long rowId;

    @Column(nullable = false)
    private long userId;

    @Column(name = "client_id", nullable = false)
    private long clientId;

    @Column(nullable = false)
    private long habitClientId;

    @Column(nullable = false)
    private long epochDay;

    @Column(nullable = false)
    private long checkedAt;
}
