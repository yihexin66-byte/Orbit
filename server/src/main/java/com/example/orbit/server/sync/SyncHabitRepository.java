package com.example.orbit.server.sync;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SyncHabitRepository extends JpaRepository<SyncHabit, Long> {

    List<SyncHabit> findByUserIdOrderBySortOrderAscClientIdAsc(long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SyncHabit habit where habit.userId = :userId")
    void deleteByUserId(@Param("userId") long userId);
}
