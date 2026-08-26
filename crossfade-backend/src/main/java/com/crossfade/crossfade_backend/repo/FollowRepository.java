package com.crossfade.crossfade_backend.repo;

import com.crossfade.crossfade_backend.model.Follow;
import com.crossfade.crossfade_backend.model.FollowId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {
    long countByFollowee_Id(Long userId);
    long countByFollower_Id(Long userId);
}
