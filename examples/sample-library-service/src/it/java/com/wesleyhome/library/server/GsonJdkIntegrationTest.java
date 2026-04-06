package com.wesleyhome.library.server;

import com.wesleyhome.library.client.gson.catalog.model.SearchCatalogInputDTO;
import com.wesleyhome.library.client.gson.catalog.model.SearchCatalogOutputDTO;
import com.wesleyhome.library.client.gson.client.GsonCodec;
import com.wesleyhome.library.client.gson.client.JdkHttpTransport;
import com.wesleyhome.library.client.gson.client.LibraryServiceClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class GsonJdkIntegrationTest extends BaseIntegrationTest {

    @Test
    public void testSearchCatalog() throws IOException {
        LibraryServiceClient client = LibraryServiceClient.builder()
                .baseUrl(getBaseUrl())
                .transport(new JdkHttpTransport())
                .codec(new GsonCodec())
                .build();

        SearchCatalogInputDTO input = new SearchCatalogInputDTO("java", null, 0, "integration-test");
        SearchCatalogOutputDTO response = client.searchCatalog(input);

        assertThat(response).isNotNull();
        assertThat(response.items()).isNotNull();
    }
}
