package com.wesleyhome.library.server;

import com.wesleyhome.library.server.config.SpringDelegateFallbackConfiguration;
import com.wesleyhome.library.server.controller.CirculationController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;

@WebMvcTest(CirculationController.class)
@AutoConfigureRestTestClient
@Import(SpringDelegateFallbackConfiguration.class)
public class CirculationControllerIntegrationTest {

    @Autowired
    private RestTestClient client;


    @Test
    void testCheckInItem() {
        client.post().uri("/circulation/checkin/loan-123")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_IMPLEMENTED.value());
    }
}
