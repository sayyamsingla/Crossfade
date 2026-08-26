package com.crossfade.crossfade_backend.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String displayName;
    private String handle;
    private String bio;
    private String avatarUrl;

    @ManyToOne
    @JoinColumn(name = "now_playing_track_id")
    private Track nowPlayingTrack;
}
