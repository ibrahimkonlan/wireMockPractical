package com.amalitech.wiremock.extension;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformerV2;
import com.github.tomakehurst.wiremock.http.ResponseDefinition;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import java.util.UUID;

public class RequestIdResponseTransformer implements ResponseDefinitionTransformerV2 {

    @Override
    public String getName() {
        return "request-id-transformer";
    }

    @Override
    public ResponseDefinition transform(ServeEvent serveEvent) {
        ResponseDefinition original = serveEvent.getResponseDefinition();
        return ResponseDefinitionBuilder.like(original)
                .withHeader("X-Request-Id", "req-" + UUID.randomUUID())
                .build();
    }
}
