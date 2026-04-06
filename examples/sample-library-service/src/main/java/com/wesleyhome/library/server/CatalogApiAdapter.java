package com.wesleyhome.library.server;

import com.wesleyhome.library.server.catalog.api.AddMediaItemApi;
import com.wesleyhome.library.server.catalog.api.GetMediaApi;
import com.wesleyhome.library.server.catalog.api.SearchCatalogApi;
import com.wesleyhome.library.server.catalog.model.*;
import com.wesleyhome.library.server.mapper.api.CatalogApiMapper;
import com.wesleyhome.library.server.service.MediaService;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class CatalogApiAdapter implements GetMediaApi, SearchCatalogApi, AddMediaItemApi {

    private final MediaService mediaService;
    private final CatalogApiMapper apiMapper;

    public CatalogApiAdapter(MediaService mediaService, CatalogApiMapper apiMapper) {
        this.mediaService = mediaService;
        this.apiMapper = apiMapper;
    }

    @Override
    public GetMediaOutputDTO getMedia(String id) {
        return mediaService.getMediaItem(id)
                .map(apiMapper::toGetMediaOutputDTO)
                .orElseThrow(() -> new RuntimeException("Media not found"));
    }

    @Override
    public SearchCatalogOutputDTO searchCatalog(String q, MediaTypeDTO type, Integer page, String source) {
        var items = mediaService.searchCatalog(q, type != null ? type.name() : null, page != null ? page : 0, 20);
        var apiItems = items.stream().map(apiMapper::toApi).collect(Collectors.toList());
        return new SearchCatalogOutputDTO(apiItems, (long) apiItems.size());
    }

    @Override
    public AddMediaItemOutputDTO addMediaItem(MediaItemDTO item) {
        var domain = apiMapper.toDomain(item);
        var saved = mediaService.addMediaItem(domain);
        return apiMapper.toAddMediaItemOutputDTO(saved);
    }
}
