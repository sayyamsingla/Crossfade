package com.crossfade.crossfade_backend.repo;

import com.crossfade.crossfade_backend.model.UserTopArtist;
import com.crossfade.crossfade_backend.model.UserTopArtistId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTopArtistRepository extends JpaRepository<UserTopArtist, UserTopArtistId> {
    List<UserTopArtist> findByUser_IdOrderByRankAsc(Long userId);
}
