package com.example.library.domain;

import java.time.OffsetDateTime;

public record Loan(
    String id,
    Patron patron,
    MediaItem item,
    OffsetDateTime dueDate,
    OffsetDateTime returnedAt,
    LoanStatus status
) {
    public boolean isOverdue() {
        return status == LoanStatus.ACTIVE && OffsetDateTime.now().isAfter(dueDate);
    }
}
