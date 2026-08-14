package com.msa4lmsv2scg.global.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void normalizesValidRequestIdAndPropagatesItToRequestAndResponse() {
        String incoming = "550E8400-E29B-41D4-A716-446655440000";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/academic/courses")
                        .header(RequestIdFilter.HEADER_NAME, incoming)
                        .build()
        );
        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, tracedExchange -> {
            forwarded.set(tracedExchange);
            return Mono.empty();
        })).verifyComplete();

        String expected = incoming.toLowerCase();
        assertThat(forwarded.get().getRequest().getHeaders().getFirst(RequestIdFilter.HEADER_NAME))
                .isEqualTo(expected);
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdFilter.HEADER_NAME))
                .isEqualTo(expected);
        String exchangeRequestId = forwarded.get().getAttribute(RequestIdFilter.EXCHANGE_ATTRIBUTE);
        assertThat(exchangeRequestId).isEqualTo(expected);
    }

    @Test
    void replacesMalformedRequestIdWithCanonicalUuid() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/payment/charges")
                        .header(RequestIdFilter.HEADER_NAME, "not-a-request-id")
                        .build()
        );
        AtomicReference<String> forwardedRequestId = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, tracedExchange -> {
            forwardedRequestId.set(
                    tracedExchange.getRequest().getHeaders().getFirst(RequestIdFilter.HEADER_NAME)
            );
            return Mono.empty();
        })).verifyComplete();

        assertThat(forwardedRequestId.get())
                .matches("^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
        assertThat(exchange.getResponse().getHeaders().getFirst(RequestIdFilter.HEADER_NAME))
                .isEqualTo(forwardedRequestId.get());
    }
}
