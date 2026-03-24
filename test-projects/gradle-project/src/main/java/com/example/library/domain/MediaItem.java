package com.example.library.domain;

import java.time.Instant;

public sealed interface MediaItem permits MediaItem.Book,
        MediaItem.Cd,
        MediaItem.Movie,
        MediaItem.Magazine {

    String id();

    default String getId() {
        return id();
    }

    String title();

    default String getTitle() {
        return title();
    }

    int availableCopies();

    int totalCopies();

    record Book(String id, String title, String author, String isbn,
                Integer pages, int availableCopies, int totalCopies) implements
            MediaItem {
    }

    record Cd(String id, String title, String artist, int trackCount,
              int availableCopies, int totalCopies) implements MediaItem {
    }

    record Movie(String id, String title, String director, int durationMinutes,
                 String rating, int availableCopies, int totalCopies) implements
            MediaItem {
    }

    record Magazine(String id, String title, String issueNumber,
                    Instant publishDate, int availableCopies,
                    int totalCopies) implements MediaItem {
    }
}
