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

import static com.amalitech.wiremock.support.TestHttpClient.httpPost;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.notMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tasks 2 & 3:
 */
class AuthAndBodyMatchingTest {

    private static final String VALID_AUTH_HEADER = "Bearer valid-token";

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @BeforeEach
    void setUp() {
        // Happy path: valid token + productId "P-100" -> 201 Created.
        wm.stubFor(post(urlEqualTo("/orders"))
                .withHeader("Authorization", equalTo(VALID_AUTH_HEADER))
                .withRequestBody(matchingJsonPath("$.productId", equalTo("P-100")))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "ord-1001",
                                  "productId": "P-100",
                                  "quantity": 3,
                                  "status": "CREATED"
                                }
                                """)));

        // Same auth, but any productId other than "P-100" -> 400 validation error.
        wm.stubFor(post(urlEqualTo("/orders"))
                .withHeader("Authorization", equalTo(VALID_AUTH_HEADER))
                .withRequestBody(matchingJsonPath("$.productId", notMatching("P-100")))
                .atPriority(1)
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"error": "validation_error", "message": "Unknown productId"}
                                """)));

        // No Authorization header at all -> 401.
        wm.stubFor(post(urlEqualTo("/orders"))
                .withHeader("Authorization", absent())
                .atPriority(2)
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"error": "unauthorized", "message": "Missing Authorization header"}
                                """)));

        // Authorization header present but not the valid token -> 401.
        wm.stubFor(post(urlEqualTo("/orders"))
                .withHeader("Authorization", notMatching(VALID_AUTH_HEADER))
                .atPriority(2)
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"error": "unauthorized", "message": "Invalid or expired token"}
                                """)));
    }

    @Test
    @DisplayName("valid token + productId P-100 -> 201 Created")
    void validTokenAndKnownProductCreatesOrder() throws IOException {
        try (Response response = httpPost(wm.baseUrl() + "/orders",
                Map.of("Authorization", VALID_AUTH_HEADER),
                """
                {"productId": "P-100", "quantity": 3}
                """)) {
            assertEquals(201, response.code());
            JsonObject body = JsonParser.parseString(response.body().string()).getAsJsonObject();
            assertEquals("CREATED", body.get("status").getAsString());
        }
    }

    @Test
    @DisplayName("valid token + unknown productId -> 400 Bad Request")
    void validTokenAndUnknownProductReturnsBadRequest() throws IOException {
        try (Response response = httpPost(wm.baseUrl() + "/orders",
                Map.of("Authorization", VALID_AUTH_HEADER),
                """
                {"productId": "P-999", "quantity": 1}
                """)) {
            assertEquals(400, response.code());
            JsonObject body = JsonParser.parseString(response.body().string()).getAsJsonObject();
            assertEquals("validation_error", body.get("error").getAsString());
        }
    }

    @Test
    @DisplayName("missing Authorization header -> 401 Unauthorized")
    void missingAuthHeaderReturnsUnauthorized() throws IOException {
        try (Response response = httpPost(wm.baseUrl() + "/orders",
                Map.of(),
                """
                {"productId": "P-100", "quantity": 1}
                """)) {
            assertEquals(401, response.code());
        }
    }

    @Test
    @DisplayName("invalid bearer token -> 401 Unauthorized")
    void invalidTokenReturnsUnauthorized() throws IOException {
        try (Response response = httpPost(wm.baseUrl() + "/orders",
                Map.of("Authorization", "Bearer not-the-right-token"),
                """
                {"productId": "P-100", "quantity": 1}
                """)) {
            assertEquals(401, response.code());
        }
    }
}
