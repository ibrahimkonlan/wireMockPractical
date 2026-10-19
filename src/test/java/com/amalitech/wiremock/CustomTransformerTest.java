package com.amalitech.wiremock;

import com.amalitech.wiremock.extension.RequestIdResponseTransformer;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.Map;

import static com.amalitech.wiremock.support.TestHttpClient.httpGet;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


class CustomTransformerTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().extensions(new RequestIdResponseTransformer()))
            .build();

    @BeforeEach
    void setUp() {
        wm.stubFor(get(urlEqualTo("/orders/123"))
                .willReturn(okJson("""
                        {"id": "123", "productId": "P-100", "quantity": 2, "status": "CONFIRMED"}
                        """)));
    }

    @Test
    @DisplayName("every response gets a computed X-Request-Id header, even though no stub defines one")
    void responsesCarryAComputedRequestIdHeader() throws IOException {
        try (Response response = httpGet(wm.baseUrl() + "/orders/123", Map.of())) {
            String requestId = response.header("X-Request-Id");
            assertNotNull(requestId);
            assertTrue(requestId.startsWith("req-"));
        }
    }
}
