package com.crossfade.crossfade_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@IdClass(UserTopArtistId.class)
public class UserTopArtist {
    @Id
    @ManyToOne
    @JoinColumn(name = "artist_id")
    private Artist artist;

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private Integer rank;

}
