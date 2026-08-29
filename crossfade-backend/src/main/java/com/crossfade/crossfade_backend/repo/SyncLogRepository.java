package com.crossfade.crossfade_backend.repo;

import com.crossfade.crossfade_backend.model.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {
    long countByUser_IdAndSyncedAtAfter(Long userId, Instant since);
    List<SyncLog> findByUser_IdAndSyncedAtAfterOrderBySyncedAtAsc(Long userId, Instant since);
}
