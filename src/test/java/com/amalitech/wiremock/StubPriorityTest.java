package com.amalitech.wiremock;

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
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 9:
 */
class StubPriorityTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @BeforeEach
    void setUp() {

        wm.stubFor(get(urlMatching("/orders/[0-9]+"))
                .atPriority(5)
                .willReturn(okJson("""
                        {"id": "unknown", "productId": "P-000", "quantity": 0, "status": "GENERIC_FALLBACK"}
                        """)));


        wm.stubFor(get(urlEqualTo("/orders/42"))
                .atPriority(1)
                .willReturn(okJson("""
                        {"id": "42", "productId": "P-999", "quantity": 9, "status": "SPECIAL_ORDER"}
                        """)));
    }

    @Test
    @DisplayName("the specific stub for /orders/42 wins over the generic numeric-ID stub")
    void specificStubTakesPrecedenceOverGenericOne() throws IOException {
        try (Response response = httpGet(wm.baseUrl() + "/orders/42", Map.of())) {
            String body = response.body().string();
            assertTrue(body.contains("SPECIAL_ORDER"), "expected the specific stub's body, got: " + body);
        }
    }

    @Test
    @DisplayName("any other numeric order ID still falls through to the generic stub")
    void otherNumericIdsHitTheGenericStub() throws IOException {
        try (Response response = httpGet(wm.baseUrl() + "/orders/7", Map.of())) {
            String body = response.body().string();
            assertTrue(body.contains("GENERIC_FALLBACK"), "expected the generic stub's body, got: " + body);
        }
    }
}
