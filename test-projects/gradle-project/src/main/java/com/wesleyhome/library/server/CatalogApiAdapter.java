package com.wesleyhome.library.server;

import com.wesleyhome.library.server.api.catalog.AddMediaItemApi;
import com.wesleyhome.library.server.api.catalog.GetMediaApi;
import com.wesleyhome.library.server.api.catalog.SearchCatalogApi;
import com.wesleyhome.library.server.model.catalog.AddMediaItemOutputDTO;
import com.wesleyhome.library.server.model.catalog.GetMediaOutputDTO;
import com.wesleyhome.library.server.model.catalog.MediaItemDTO;
import com.wesleyhome.library.server.model.catalog.MediaTypeDTO;
import com.wesleyhome.library.server.model.catalog.SearchCatalogOutputDTO;
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
