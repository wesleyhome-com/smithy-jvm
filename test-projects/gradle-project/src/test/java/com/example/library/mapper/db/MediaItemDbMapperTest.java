package com.example.library.mapper.db;

import com.example.library.db.tables.records.MediaItemsRecord;
import com.example.library.domain.MediaItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MediaItemDbMapperTest {

    @Autowired
    private MediaItemDbMapper mapper;

    @Test
    void toDomain_shouldMapBookCorrectly() {
        MediaItemsRecord record = new MediaItemsRecord();
        record.setId("B1");
        record.setMediaType("BOOK");
        record.setTitle("Test Book");
        record.setAuthorArtistDirector("Test Author");
        record.setIsbn("12345");
        record.setPages(300);

        MediaItem result = mapper.toDomain(record);

        assertThat(result).isInstanceOf(MediaItem.Book.class);
        MediaItem.Book book = (MediaItem.Book) result;
        assertThat(book.id()).isEqualTo("B1");
        assertThat(book.title()).isEqualTo("Test Book");
        assertThat(book.author()).isEqualTo("Test Author");
        assertThat(book.isbn()).isEqualTo("12345");
        assertThat(book.pages()).isEqualTo(300);
    }

    @Test
    void toRecord_shouldMapBookToRecordCorrectly() {
        MediaItem.Book book = new MediaItem.Book("B1", "Test Book", "Test Author", "12345", 300, 5, 5);

        MediaItemsRecord result = mapper.toRecord(book);

        assertThat(result.getId()).isEqualTo("B1");
        assertThat(result.getMediaType()).isEqualTo("BOOK");
        assertThat(result.getTitle()).isEqualTo("Test Book");
        assertThat(result.getAuthorArtistDirector()).isEqualTo("Test Author");
        assertThat(result.getIsbn()).isEqualTo("12345");
        assertThat(result.getPages()).isEqualTo(300);
    }

    @Test
    void toDomain_shouldMapCdCorrectly() {
        MediaItemsRecord record = new MediaItemsRecord();
        record.setMediaType("CD");
        record.setTitle("Test CD");
        record.setAuthorArtistDirector("Test Artist");
        record.setPages(12); // trackCount stored in pages

        MediaItem result = mapper.toDomain(record);

        assertThat(result).isInstanceOf(MediaItem.Cd.class);
        MediaItem.Cd cd = (MediaItem.Cd) result;
        assertThat(cd.title()).isEqualTo("Test CD");
        assertThat(cd.artist()).isEqualTo("Test Artist");
        assertThat(cd.trackCount()).isEqualTo(12);
    }
}
