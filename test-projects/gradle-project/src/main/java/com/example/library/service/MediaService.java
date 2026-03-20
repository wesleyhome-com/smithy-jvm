package com.example.library.service;

import com.example.library.db.tables.records.MediaItemsRecord;
import com.example.library.domain.MediaItem;
import com.example.library.mapper.db.MediaItemDbMapper;
import com.example.library.repository.MediaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MediaService {

    private final MediaRepository mediaRepository;
    private final MediaItemDbMapper mediaItemMapper;

    public MediaService(MediaRepository mediaRepository, MediaItemDbMapper mediaItemMapper) {
        this.mediaRepository = mediaRepository;
        this.mediaItemMapper = mediaItemMapper;
    }

    public Optional<MediaItem> getMediaItem(String id) {
        return mediaRepository.findById(id)
                .map(mediaItemMapper::toDomain);
    }

    public List<MediaItem> searchCatalog(String query, String type, int page, int size) {
        int offset = page * size;
        return mediaRepository.findAll(query, type, size, offset).stream()
                .map(mediaItemMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Transactional
    public MediaItem addMediaItem(MediaItem item) {
        String id = item.id() != null ? item.id() : UUID.randomUUID().toString();
        
        // Use mapper to populate the record from domain
        MediaItemsRecord record = mediaItemMapper.toRecord(item);
        record.setId(id); // Ensure ID is set
        mediaRepository.save(record);
        // Return the saved state
        return getMediaItem(id).orElseThrow();
    }
}
