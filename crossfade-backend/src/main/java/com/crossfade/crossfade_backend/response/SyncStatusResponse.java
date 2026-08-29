package com.crossfade.crossfade_backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatusResponse {
    private int syncsUsed;
    private int syncsAllowed;
    private Instant nextAvailableAt;
}
