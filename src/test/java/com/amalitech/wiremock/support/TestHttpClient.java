package com.amalitech.wiremock.support;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Thin OkHttp wrapper so tests drive the mock server the way a real client would
 */
public final class TestHttpClient {

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(2))
            .readTimeout(Duration.ofSeconds(10))
            .build();

    private TestHttpClient() {
    }

    public static Response httpGet(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        headers.forEach(builder::addHeader);
        return CLIENT.newCall(builder.build()).execute();
    }

    public static Response httpPost(String url, Map<String, String> headers, String jsonBody) throws IOException {
        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));
        Request.Builder builder = new Request.Builder().url(url).post(body);
        headers.forEach(builder::addHeader);
        return CLIENT.newCall(builder.build()).execute();
    }

    public static Response httpDelete(String url, Map<String, String> headers) throws IOException {
        Request.Builder builder = new Request.Builder().url(url).delete();
        headers.forEach(builder::addHeader);
        return CLIENT.newCall(builder.build()).execute();
    }
}
