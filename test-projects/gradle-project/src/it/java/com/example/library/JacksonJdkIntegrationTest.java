package com.example.library;

import com.example.library.client.jackson.client.LibraryServiceClient;
import com.example.library.client.jackson.model.catalog.SearchCatalogInputDTO;
import com.example.library.client.jackson.model.catalog.SearchCatalogOutputDTO;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JacksonJdkIntegrationTest extends BaseIntegrationTest {

    @Test
    public void testSearchCatalog() throws IOException {
        LibraryServiceClient client = LibraryServiceClient.create(getBaseUrl());
        
        SearchCatalogInputDTO input = new SearchCatalogInputDTO("java", null, 0);
        SearchCatalogOutputDTO response = client.searchCatalog(input);

        assertThat(response).isNotNull();
        assertThat(response.items()).isNotNull();
    }
}
