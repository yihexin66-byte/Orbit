package com.example.orbit.server.sync;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SyncRevisionRepository extends JpaRepository<SyncRevision, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from SyncRevision r where r.userId = :userId")
    Optional<SyncRevision> findForUpdate(@Param("userId") long userId);
}
