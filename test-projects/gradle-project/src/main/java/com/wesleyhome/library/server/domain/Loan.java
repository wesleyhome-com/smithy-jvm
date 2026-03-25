package com.wesleyhome.library.server.domain;

import java.time.Instant;

public record Loan(
    String id,
    Patron patron,
    MediaItem item,
    Instant dueDate,
    Instant returnedAt,
    LoanStatus status
) {
    public boolean isOverdue() {
        return status == LoanStatus.ACTIVE && Instant.now().isAfter(dueDate);
    }
}
