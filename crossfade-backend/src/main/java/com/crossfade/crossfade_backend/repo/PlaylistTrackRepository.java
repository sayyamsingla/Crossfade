package com.crossfade.crossfade_backend.repo;

import com.crossfade.crossfade_backend.model.PlaylistTrack;
import com.crossfade.crossfade_backend.model.PlaylistTrackId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistTrackRepository extends JpaRepository<PlaylistTrack, PlaylistTrackId> {
    long countByPlaylist_Id(Long playlistId);
}
