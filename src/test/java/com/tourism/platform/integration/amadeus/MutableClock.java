package com.tourism.platform.integration.amadeus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

/** Test-only clock so token cache expiry can be exercised deterministically. */
final class MutableClock extends Clock {

    private final ZoneId zone;
    private Instant instant;

    MutableClock(Instant start, ZoneId zone) {
        this.instant = start;
        this.zone = zone;
    }

    void setInstant(Instant instant) {
        this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    @Override
    public Instant instant() {
        return instant;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
    }
}
