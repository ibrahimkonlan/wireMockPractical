package com.amalitech.wiremock;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.Map;

import static com.amalitech.wiremock.support.TestHttpClient.httpPost;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Task 8:
 */
class VerificationTest {

    private static final String ORDER_BODY = """
            {"productId": "P-100", "quantity": 3}
            """;

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @BeforeEach
    void setUp() {
        wm.stubFor(post(urlEqualTo("/orders"))
                .willReturn(aResponse().withStatus(201)));
    }

    @Test
    @DisplayName("POST /orders was called exactly once with the expected body")
    void postOrdersCalledExactlyOnceWithExpectedBody() throws IOException {
        try (Response response = httpPost(wm.baseUrl() + "/orders", Map.of(), ORDER_BODY)) {
            response.close();
        }

        wm.verify(1, postRequestedFor(urlEqualTo("/orders"))
                .withRequestBody(equalToJson(ORDER_BODY)));
    }

    @Test
    @DisplayName("DELETE /orders/789 was never called (negative verification)")
    void deleteOrders789WasNeverCalled() {
        wm.verify(0, deleteRequestedFor(urlEqualTo("/orders/789")));
    }
}
