package com.wesleyhome.library.server.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public record Reservation(
    String id,
    Patron patron,
    String resourceId,
    Instant startTime,
    int durationMinutes,
    ReservationStatus status
) {
    public Instant endTime() {
        return startTime.plus(durationMinutes, ChronoUnit.MINUTES);
    }
}
