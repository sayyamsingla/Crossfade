package com.crossfade.crossfade_backend.model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@AllArgsConstructor
@NoArgsConstructor
public class PlaylistTrackId implements Serializable {
    private Long playlist;
    private Long track;

}
