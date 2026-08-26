    package com.crossfade.crossfade_backend.repo;

    import com.crossfade.crossfade_backend.model.Track;
    import org.springframework.data.jpa.repository.JpaRepository;


    public interface TrackRepository extends JpaRepository<Track, Long> {
    }
