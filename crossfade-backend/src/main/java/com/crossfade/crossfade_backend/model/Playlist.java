package com.crossfade.crossfade_backend.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 1000)
    private String coverUrl;

    @Column(unique = true)
    private String spotifyId;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

}
