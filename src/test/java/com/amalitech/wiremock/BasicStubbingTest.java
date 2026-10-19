package com.amalitech.wiremock;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Task 1:
 */
class BasicStubbingTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @BeforeEach
    void setUp() {
        wm.stubFor(get(urlEqualTo("/orders/123"))
                .willReturn(okJson("""
                        {
                          "id": "123",
                          "productId": "P-100",
                          "quantity": 2,
                          "status": "CONFIRMED"
                        }
                        """)));
    }

    @Test
    @DisplayName("GET /orders/123 returns 200 with the full order representation")
    void returnsOrderById() throws IOException {
        try (Response response = httpGet(wm.baseUrl() + "/orders/123", Map.of())) {
            assertEquals(200, response.code());

            JsonObject body = JsonParser.parseString(response.body().string()).getAsJsonObject();
            assertEquals("123", body.get("id").getAsString());
            assertEquals("P-100", body.get("productId").getAsString());
            assertEquals(2, body.get("quantity").getAsInt());
            assertEquals("CONFIRMED", body.get("status").getAsString());
        }
    }
}
