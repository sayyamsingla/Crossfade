package com.crossfade.crossfade_backend.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@IdClass(FollowId.class)
@Entity
@Data
public class Follow {

    @Id
    @ManyToOne
    @JoinColumn(name = "followee_id")
    private User followee;


    @Id
    @ManyToOne
    @JoinColumn(name = "follower_id")
    private User follower;

    private Instant createdAt;


}
