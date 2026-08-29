    package com.crossfade.crossfade_backend.repo;

    import com.crossfade.crossfade_backend.model.Track;
    import org.springframework.data.jpa.repository.JpaRepository;

    import java.util.Optional;


    public interface TrackRepository extends JpaRepository<Track, Long> {
        Optional<Track> findBySpotifyId(String spotifyId);
    }
