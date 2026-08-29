package com.crossfade.crossfade_backend.repo;

import com.crossfade.crossfade_backend.model.PlaylistLike;
import com.crossfade.crossfade_backend.model.PlaylistLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaylistLikeRepository extends JpaRepository<PlaylistLike, PlaylistLikeId> {
    long countByPlaylist_Id(Long playlistId);
    boolean existsByPlaylist_IdAndUser_Id(Long playlistId, Long userId);
    void deleteByPlaylist_IdAndUser_Id(Long playlistId, Long userId);
}
