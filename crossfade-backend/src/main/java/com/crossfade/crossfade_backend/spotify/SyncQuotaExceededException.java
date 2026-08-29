package com.crossfade.crossfade_backend.spotify;

import java.time.Instant;

public class SyncQuotaExceededException extends RuntimeException {

    private final Instant nextAvailableAt;

    public SyncQuotaExceededException(Instant nextAvailableAt) {
        super("Sync quota exceeded. Next available at " + nextAvailableAt);
        this.nextAvailableAt = nextAvailableAt;
    }

    public Instant getNextAvailableAt() {
        return nextAvailableAt;
    }
}
