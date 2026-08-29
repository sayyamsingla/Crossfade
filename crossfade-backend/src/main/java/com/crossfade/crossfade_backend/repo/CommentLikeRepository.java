package com.crossfade.crossfade_backend.repo;

import com.crossfade.crossfade_backend.model.CommentLike;
import com.crossfade.crossfade_backend.model.CommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {
    long countByComment_Id(Long commentId);
    boolean existsByComment_IdAndUser_Id(Long commentId, Long userId);
    void deleteByComment_IdAndUser_Id(Long commentId, Long userId);
}
