package com.crossfade.crossfade_backend.repo;

import com.crossfade.crossfade_backend.model.UserTopTrack;
import com.crossfade.crossfade_backend.model.UserTopTrackId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTopTrackRepository extends JpaRepository<UserTopTrack, UserTopTrackId> {
    List<UserTopTrack> findByUser_IdOrderByRankAsc(Long userId);
}
