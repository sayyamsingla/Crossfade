package com.crossfade.crossfade_backend.repo;

import com.crossfade.crossfade_backend.model.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {
    List<Playlist> findByOwner_Id(Long ownerId);
}
