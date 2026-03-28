package com.wesleyhome.library.server.mapper.db;

import com.wesleyhome.library.db.tables.records.MediaItemsRecord;
import com.wesleyhome.library.server.domain.MediaItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CommonDbMapper.class}
)
public interface MediaItemDbMapper {

    // Main entry point: DB Record -> Domain Model
    default MediaItem toDomain(MediaItemsRecord record) {
        if (record == null) return null;
        String type = record.getMediaType();
        return switch (type) {
            case "BOOK" -> mapToBook(record);
            case "CD" -> mapToCd(record);
            case "MOVIE" -> mapToMovie(record);
            case "MAGAZINE" -> mapToMagazine(record);
            default -> throw new IllegalArgumentException("Unknown media type in database: " + type);
        };
    }

    // --- Sub-type Mappings (DB to Domain) ---

    @Mapping(target = "author", source = "authorArtistDirector")
    MediaItem.Book mapToBook(MediaItemsRecord record);

    @Mapping(target = "artist", source = "authorArtistDirector")
    @Mapping(target = "trackCount", source = "pages")
    MediaItem.Cd mapToCd(MediaItemsRecord record);

    @Mapping(target = "director", source = "authorArtistDirector")
    MediaItem.Movie mapToMovie(MediaItemsRecord record);

    MediaItem.Magazine mapToMagazine(MediaItemsRecord record);


    // --- Reverse Mappings (Domain Model -> DB Record) ---
    default MediaItemsRecord toRecord(MediaItem domain) {
        if (domain == null) return null;
        return switch (domain) {
            case MediaItem.Cd cd -> mapCdToRecord(cd);
            case MediaItem.Book book -> mapBookToRecord(book);
            case MediaItem.Magazine magazine -> mapMagazineToRecord(magazine);
            case MediaItem.Movie movie -> mapMovieToRecord(movie);
        };
    }

    @Mapping(target = "mediaType", constant = "BOOK")
    @Mapping(target = "authorArtistDirector", source = "author")
    MediaItemsRecord mapBookToRecord(MediaItem.Book domain);

    @Mapping(target = "mediaType", constant = "CD")
    @Mapping(target = "authorArtistDirector", source = "artist")
    @Mapping(target = "pages", source = "trackCount")
    MediaItemsRecord mapCdToRecord(MediaItem.Cd domain);

    @Mapping(target = "mediaType", constant = "MOVIE")
    @Mapping(target = "authorArtistDirector", source = "director")
    MediaItemsRecord mapMovieToRecord(MediaItem.Movie domain);

    @Mapping(target = "mediaType", constant = "MAGAZINE")
    MediaItemsRecord mapMagazineToRecord(MediaItem.Magazine domain);
}
