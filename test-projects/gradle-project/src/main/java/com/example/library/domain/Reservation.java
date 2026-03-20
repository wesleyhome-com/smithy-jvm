package com.example.library.domain;

import java.time.OffsetDateTime;

public record Reservation(
    String id,
    Patron patron,
    String resourceId,
    OffsetDateTime startTime,
    int durationMinutes,
    ReservationStatus status
) {
    public OffsetDateTime endTime() {
        return startTime.plusMinutes(durationMinutes);
    }
}
