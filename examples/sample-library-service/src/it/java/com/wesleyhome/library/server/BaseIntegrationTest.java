package com.wesleyhome.library.server;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {
    @LocalServerPort
    protected int port;

    protected String getBaseUrl() {
        return "http://localhost:" + port;
    }
}
