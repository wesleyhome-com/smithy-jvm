package com.wesleyhome.library.server;

import com.wesleyhome.library.client.jackson.catalog.model.SearchCatalogInputDTO;
import com.wesleyhome.library.client.jackson.catalog.model.SearchCatalogOutputDTO;
import com.wesleyhome.library.client.jackson.client.JacksonCodec;
import com.wesleyhome.library.client.jackson.client.JdkHttpTransport;
import com.wesleyhome.library.client.jackson.client.LibraryServiceClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class JacksonJdkIntegrationTest extends BaseIntegrationTest {

    @Test
    public void testSearchCatalog() throws IOException {
        LibraryServiceClient client = LibraryServiceClient.builder()
                .baseUrl(getBaseUrl())
                .transport(new JdkHttpTransport())
                .codec(new JacksonCodec())
                .build();

        SearchCatalogInputDTO input = new SearchCatalogInputDTO("java", null, 0, "integration-test");
        SearchCatalogOutputDTO response = client.searchCatalog(input);

        assertThat(response).isNotNull();
        assertThat(response.items()).isNotNull();
        // total is now an @httpHeader
        System.out.println("Total is: " + response.total());
        assertThat(response.total()).isNotNull().isGreaterThanOrEqualTo(0L);
    }
}
