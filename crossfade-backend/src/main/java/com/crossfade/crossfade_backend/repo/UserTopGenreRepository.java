package com.crossfade.crossfade_backend.repo;


import com.crossfade.crossfade_backend.model.UserTopGenre;
import com.crossfade.crossfade_backend.model.UserTopGenreId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTopGenreRepository extends JpaRepository<UserTopGenre, UserTopGenreId> {
    List<UserTopGenre> findByUser_IdOrderByPercentageDesc(Long userId);
}
