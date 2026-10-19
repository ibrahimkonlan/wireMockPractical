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
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;


class ResponseTemplatingTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort().globalTemplating(true))
            .build();

    @BeforeEach
    void setUp() {
        wm.stubFor(get(urlPathMatching("/orders/[0-9]+"))
                .willReturn(okJson("""
                        {
                          "id": "{{request.pathSegments.[1]}}",
                          "productId": "P-100",
                          "quantity": 1,
                          "status": "CONFIRMED"
                        }
                        """)));
    }

    @Test
    @DisplayName("response body echoes back whichever numeric ID was requested")
    void echoesRequestedIdBackInBody() throws IOException {
        try (Response response = httpGet(wm.baseUrl() + "/orders/555", Map.of())) {
            assertEquals(200, response.code());
            JsonObject body = JsonParser.parseString(response.body().string()).getAsJsonObject();
            assertEquals("555", body.get("id").getAsString());
        }

        try (Response response = httpGet(wm.baseUrl() + "/orders/777", Map.of())) {
            assertEquals(200, response.code());
            JsonObject body = JsonParser.parseString(response.body().string()).getAsJsonObject();
            assertEquals("777", body.get("id").getAsString());
        }
    }
}
