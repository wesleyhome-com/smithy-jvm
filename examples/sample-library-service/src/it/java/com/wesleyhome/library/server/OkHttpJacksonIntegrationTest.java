package com.wesleyhome.library.server;

import com.wesleyhome.library.client.okhttp.catalog.model.SearchCatalogInputDTO;
import com.wesleyhome.library.client.okhttp.catalog.model.SearchCatalogOutputDTO;
import com.wesleyhome.library.client.okhttp.client.JacksonCodec;
import com.wesleyhome.library.client.okhttp.client.LibraryServiceClient;
import com.wesleyhome.library.client.okhttp.client.OkHttpTransport;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class OkHttpJacksonIntegrationTest
        extends BaseIntegrationTest {

    @Test
    public void testSearchCatalog()
            throws IOException {
        LibraryServiceClient client = LibraryServiceClient.builder()
                .baseUrl(getBaseUrl())
                .transport(new OkHttpTransport())
                .codec(new JacksonCodec())
                .build();

        SearchCatalogInputDTO input = new SearchCatalogInputDTO("java", null, 0, "integration-test");
        SearchCatalogOutputDTO response = client.searchCatalog(input);

        assertThat(response).isNotNull();
        assertThat(response.items()).isNotNull();
    }
}
