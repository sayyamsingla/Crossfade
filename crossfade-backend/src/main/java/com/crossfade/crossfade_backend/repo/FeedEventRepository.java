package com.crossfade.crossfade_backend.repo;

import com.crossfade.crossfade_backend.model.FeedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedEventRepository extends JpaRepository<FeedEvent, Long> {
    List<FeedEvent> findByActor_IdInOrderByCreatedAtDesc(List<Long> actorIds);
}
