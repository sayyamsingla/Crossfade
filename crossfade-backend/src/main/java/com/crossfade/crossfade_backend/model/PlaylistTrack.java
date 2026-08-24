package com.crossfade.crossfade_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@IdClass(PlaylistTrackId.class)
public class PlaylistTrack {

    @Id
    @ManyToOne
    @JoinColumn(name = "playlist_id")
    private Playlist playlist;

    @Id
    @ManyToOne
    @JoinColumn(name = "track_id")
    private Track track;

    private Integer position;

}
