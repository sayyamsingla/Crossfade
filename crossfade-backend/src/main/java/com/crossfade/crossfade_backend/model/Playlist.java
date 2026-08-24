package com.crossfade.crossfade_backend.model;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Playlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String coverUrl;

}
