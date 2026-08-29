package com.crossfade.crossfade_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;

    @Column(length = 1000)
    private String coverUrl;

    @Column(unique = true)
    private String spotifyId;

    @ManyToOne
    @JoinColumn(name = "artist_id")
    private Artist artist;

}
