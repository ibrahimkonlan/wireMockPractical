package com.amalitech.wiremock;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.Map;

import static com.amalitech.wiremock.support.TestHttpClient.httpDelete;
import static com.amalitech.wiremock.support.TestHttpClient.httpGet;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 7:

 */
class OrderCancellationScenarioTest {

    private static final String SCENARIO_NAME = "order-456-cancellation";
    private static final String STATE_CANCELLING = "cancelling";

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @BeforeEach
    void setUp() {
        wm.stubFor(delete(urlEqualTo("/orders/456"))
                .inScenario(SCENARIO_NAME)
                .whenScenarioStateIs(STARTED)
                .willSetStateTo(STATE_CANCELLING)
                .willReturn(aResponse().withStatus(202)));

        wm.stubFor(get(urlEqualTo("/orders/456"))
                .inScenario(SCENARIO_NAME)
                .whenScenarioStateIs(STATE_CANCELLING)
                .willReturn(okJson("""
                        {"id": "456", "productId": "P-100", "quantity": 1, "status": "CANCELLING"}
                        """)));

        wm.stubFor(delete(urlEqualTo("/orders/456"))
                .inScenario(SCENARIO_NAME)
                .whenScenarioStateIs(STATE_CANCELLING)
                .willReturn(aResponse().withStatus(409)));
    }

    @Test
    @DisplayName("cancel -> get -> cancel again follows the documented state machine")
    void fullCancellationFlow() throws IOException {
        try (Response first = httpDelete(wm.baseUrl() + "/orders/456", Map.of())) {
            assertEquals(202, first.code());
        }

        try (Response getAfterCancel = httpGet(wm.baseUrl() + "/orders/456", Map.of())) {
            assertEquals(200, getAfterCancel.code());
            assertTrue(getAfterCancel.body().string().contains("CANCELLING"));
        }

        try (Response second = httpDelete(wm.baseUrl() + "/orders/456", Map.of())) {
            assertEquals(409, second.code());
        }
    }
}
