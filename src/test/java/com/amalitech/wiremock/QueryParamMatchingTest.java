package com.amalitech.wiremock;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.Map;

import static com.amalitech.wiremock.support.TestHttpClient.httpGet;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 4:
 */
class QueryParamMatchingTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @BeforeEach
    void setUp() {
        wm.stubFor(get(urlPathEqualTo("/orders"))
                .withQueryParam("status", equalTo("PENDING"))
                .willReturn(okJson("""
                        [
                          {"id": "201", "productId": "P-100", "quantity": 1, "status": "PENDING"},
                          {"id": "202", "productId": "P-200", "quantity": 5, "status": "PENDING"}
                        ]
                        """)));

        wm.stubFor(get(urlPathEqualTo("/orders"))
                .withQueryParam("status", equalTo("CANCELLED"))
                .willReturn(okJson("[]")));
    }

    @Test
    @DisplayName("GET /orders?status=PENDING returns at least two PENDING orders")
    void pendingOrdersAreReturned() throws IOException {
        try (Response response = httpGet(wm.baseUrl() + "/orders?status=PENDING", Map.of())) {
            assertEquals(200, response.code());
            JsonArray orders = JsonParser.parseString(response.body().string()).getAsJsonArray();
            assertTrue(orders.size() >= 2);
            orders.forEach(order -> assertEquals("PENDING",
                    order.getAsJsonObject().get("status").getAsString()));
        }
    }

    @Test
    @DisplayName("GET /orders?status=CANCELLED returns a different (empty) result")
    void cancelledOrdersAreDifferentFromPending() throws IOException {
        try (Response response = httpGet(wm.baseUrl() + "/orders?status=CANCELLED", Map.of())) {
            assertEquals(200, response.code());
            JsonArray orders = JsonParser.parseString(response.body().string()).getAsJsonArray();
            assertEquals(0, orders.size());
        }
    }
}
