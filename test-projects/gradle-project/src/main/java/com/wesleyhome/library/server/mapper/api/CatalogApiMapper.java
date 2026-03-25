package com.wesleyhome.library.server.mapper.api;

import com.wesleyhome.library.server.domain.MediaItem;
import com.wesleyhome.library.server.model.catalog.AddMediaItemOutputDTO;
import com.wesleyhome.library.server.model.catalog.BookDetailsDTO;
import com.wesleyhome.library.server.model.catalog.CdDetailsDTO;
import com.wesleyhome.library.server.model.catalog.GetMediaOutputDTO;
import com.wesleyhome.library.server.model.catalog.MagazineDetailsDTO;
import com.wesleyhome.library.server.model.catalog.MovieDetailsDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.SubclassMapping;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CatalogApiMapper {

    // --- Domain -> API ---

    @SubclassMapping(source = MediaItem.Book.class, target = com.wesleyhome.library.server.model.catalog.MediaItemDTO.Book.class)
    @SubclassMapping(source = MediaItem.Magazine.class, target = com.wesleyhome.library.server.model.catalog.MediaItemDTO.Magazine.class)
    @SubclassMapping(source = MediaItem.Cd.class, target = com.wesleyhome.library.server.model.catalog.MediaItemDTO.Cd.class)
    @SubclassMapping(source = MediaItem.Movie.class, target = com.wesleyhome.library.server.model.catalog.MediaItemDTO.Movie.class)
    com.wesleyhome.library.server.model.catalog.MediaItemDTO toApi(MediaItem domain);

    default com.wesleyhome.library.server.model.catalog.MediaItemDTO.Book mapToApiBook(MediaItem.Book domain) {
        return new com.wesleyhome.library.server.model.catalog.MediaItemDTO.Book(toBookDetailsDTO(domain));
    }

    default com.wesleyhome.library.server.model.catalog.MediaItemDTO.Magazine mapToApiMagazine(MediaItem.Magazine domain) {
        return new com.wesleyhome.library.server.model.catalog.MediaItemDTO.Magazine(toMagazineDetailsDTO(domain));
    }

    default com.wesleyhome.library.server.model.catalog.MediaItemDTO.Cd mapToApiCd(MediaItem.Cd domain) {
        return new com.wesleyhome.library.server.model.catalog.MediaItemDTO.Cd(toCdDetailsDTO(domain));
    }

    default com.wesleyhome.library.server.model.catalog.MediaItemDTO.Movie mapToApiMovie(MediaItem.Movie domain) {
        return new com.wesleyhome.library.server.model.catalog.MediaItemDTO.Movie(toMovieDetailsDTO(domain));
    }

    BookDetailsDTO toBookDetailsDTO(MediaItem.Book domain);
    MagazineDetailsDTO toMagazineDetailsDTO(MediaItem.Magazine domain);
    CdDetailsDTO toCdDetailsDTO(MediaItem.Cd domain);
    MovieDetailsDTO toMovieDetailsDTO(MediaItem.Movie domain);


    // --- API -> Domain ---

    default MediaItem toDomain(com.wesleyhome.library.server.model.catalog.MediaItemDTO api) {
        if (api == null) return null;
        return switch (api) {
            case com.wesleyhome.library.server.model.catalog.MediaItemDTO.Book b -> mapToDomainBook(b);
            case com.wesleyhome.library.server.model.catalog.MediaItemDTO.Magazine m -> mapToDomainMagazine(m);
            case com.wesleyhome.library.server.model.catalog.MediaItemDTO.Cd c -> mapToDomainCd(c);
            case com.wesleyhome.library.server.model.catalog.MediaItemDTO.Movie mov -> mapToDomainMovie(mov);
            default -> throw new IllegalArgumentException("Unknown API media type");
        };
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "availableCopies", constant = "0")
    @Mapping(target = "totalCopies", constant = "0")
    @Mapping(target = ".", source = "value")
    MediaItem.Book mapToDomainBook(com.wesleyhome.library.server.model.catalog.MediaItemDTO.Book api);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "availableCopies", constant = "0")
    @Mapping(target = "totalCopies", constant = "0")
    @Mapping(target = ".", source = "value")
    MediaItem.Magazine mapToDomainMagazine(com.wesleyhome.library.server.model.catalog.MediaItemDTO.Magazine api);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "availableCopies", constant = "0")
    @Mapping(target = "totalCopies", constant = "0")
    @Mapping(target = ".", source = "value")
    MediaItem.Cd mapToDomainCd(com.wesleyhome.library.server.model.catalog.MediaItemDTO.Cd api);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "availableCopies", constant = "0")
    @Mapping(target = "totalCopies", constant = "0")
    @Mapping(target = ".", source = "value")
    MediaItem.Movie mapToDomainMovie(com.wesleyhome.library.server.model.catalog.MediaItemDTO.Movie api);
    
    // Output Wrappers
    @Mapping(target = "item", source = ".")
    GetMediaOutputDTO toGetMediaOutputDTO(MediaItem domain);
    
    @Mapping(target = "item", source = ".")
    AddMediaItemOutputDTO toAddMediaItemOutputDTO(MediaItem domain);
}
