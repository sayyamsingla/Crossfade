package com.crossfade.crossfade_backend.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class CommentLikeId implements Serializable {
    private Long comment;
    private Long user;
}
