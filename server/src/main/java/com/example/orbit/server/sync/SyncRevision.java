package com.example.orbit.server.sync;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sync_revisions")
@Getter
@Setter
@NoArgsConstructor
public class SyncRevision {

    @Id
    private Long userId;

    @Column(nullable = false)
    private long revision;

    @Column(nullable = false)
    private long updatedAt;
}
