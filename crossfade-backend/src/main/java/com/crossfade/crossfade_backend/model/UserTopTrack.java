package com.crossfade.crossfade_backend.model;


import jakarta.persistence.*;
import lombok.Data;

@Entity
@IdClass(UserTopTrackId.class)
@Data
public class UserTopTrack {


    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @ManyToOne
    @JoinColumn(name = "track_id")
    private Track track;

    private Integer rank;
    private Integer playCount;

}
