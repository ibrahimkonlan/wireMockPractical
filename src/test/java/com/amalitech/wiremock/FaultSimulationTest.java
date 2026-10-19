package com.amalitech.wiremock;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import okhttp3.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.util.Map;

import static com.amalitech.wiremock.support.TestHttpClient.httpGet;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Task 6:
 */
class FaultSimulationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @BeforeEach
    void resetStubs() {
        wm.resetAll();
    }

    @Test
    @DisplayName("fixed delay: server is slow but still returns a well-formed error response")
    void fixedDelaySimulatesADegradedButAliveService() throws IOException {
        wm.stubFor(get(urlEqualTo("/orders/999"))
                .willReturn(aResponse()
                        .withFixedDelay(3000)
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"error": "service_unavailable", "message": "Order service is overloaded"}
                                """)));

        long start = System.currentTimeMillis();
        try (Response response = httpGet(wm.baseUrl() + "/orders/999", Map.of())) {
            long elapsedMs = System.currentTimeMillis() - start;
            assertEquals(500, response.code());
            assertTrue(elapsedMs >= 3000, "expected at least a 3s delay, took " + elapsedMs + "ms");
        }
    }

    @Test
    @DisplayName("connection reset: the client never gets an HTTP response at all")
    void connectionResetSimulatesAHardOutage() {
        wm.stubFor(get(urlEqualTo("/orders/999"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertThrows(IOException.class, () -> httpGet(wm.baseUrl() + "/orders/999", Map.of()));
    }
}
