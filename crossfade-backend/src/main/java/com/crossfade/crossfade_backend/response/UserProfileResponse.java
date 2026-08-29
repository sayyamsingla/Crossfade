package com.crossfade.crossfade_backend.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {
    private Long id;
    private String displayName;
    private String handle;
    private String bio;
    private String avatarUrl;
    private Long followersCount;
    private Long followingCount;
    private String topGenre;
    private Boolean hasChosenHandle;
}
