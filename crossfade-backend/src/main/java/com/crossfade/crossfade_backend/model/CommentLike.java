package com.crossfade.crossfade_backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@IdClass(CommentLikeId.class)
public class CommentLike {

    @Id
    @ManyToOne
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @Id
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
