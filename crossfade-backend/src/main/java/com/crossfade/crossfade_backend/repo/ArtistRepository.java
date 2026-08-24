package com.crossfade.crossfade_backend.repo;

import com.crossfade.crossfade_backend.model.Artist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistRepository extends JpaRepository<Artist, Long> {
}
