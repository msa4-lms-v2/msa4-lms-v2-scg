package com.msa4lmsv2scg.global.filter;

import com.msa4lmsv2scg.global.resilience.GatewayResilienceProperties;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

class ResilienceFilterTest {

    @Test
    void appliesRouteSpecificTimeout() {
        ResilienceFilter filter = new ResilienceFilter(properties(1, 2, Duration.ofMillis(30)));
        MockServerWebExchange exchange = exchange("auth");

        StepVerifier.create(filter.filter(exchange, ignored -> Mono.never()))
                .expectError(TimeoutException.class)
                .verify(Duration.ofSeconds(1));
    }

    @Test
    void rejectsConcurrentRequestWhenBulkheadIsFull() {
        ResilienceFilter filter = new ResilienceFilter(properties(1, 2, Duration.ofSeconds(5)));
        MockServerWebExchange firstExchange = exchange("auth");
        Disposable firstRequest = filter.filter(firstExchange, ignored -> Mono.never()).subscribe();

        try {
            StepVerifier.create(filter.filter(exchange("auth"), ignored -> Mono.never()))
                    .expectError(BulkheadFullException.class)
                    .verify(Duration.ofSeconds(1));
        } finally {
            firstRequest.dispose();
        }
    }

    @Test
    void opensCircuitAfterConfiguredFailureWindow() {
        ResilienceFilter filter = new ResilienceFilter(properties(5, 2, Duration.ofSeconds(1)));

        for (int i = 0; i < 2; i++) {
            StepVerifier.create(filter.filter(
                            exchange("auth"),
                            ignored -> Mono.error(new IllegalStateException("downstream failure"))
                    ))
                    .expectError(IllegalStateException.class)
                    .verify();
        }

        StepVerifier.create(filter.filter(exchange("auth"), ignored -> Mono.empty()))
                .expectError(CallNotPermittedException.class)
                .verify();
    }

    private GatewayResilienceProperties properties(
            int maxConcurrentCalls,
            int minimumNumberOfCalls,
            Duration timeout
    ) {
        GatewayResilienceProperties.Profile profile = new GatewayResilienceProperties.Profile(
                timeout,
                maxConcurrentCalls,
                50,
                minimumNumberOfCalls,
                minimumNumberOfCalls,
                Duration.ofSeconds(30)
        );
        return new GatewayResilienceProperties(Map.of("auth", profile));
    }

    private MockServerWebExchange exchange(String profileName) {
        Route route = mock(Route.class);
        when(route.getId()).thenReturn("auth-service");
        when(route.getMetadata()).thenReturn(Map.of("resilience-profile", profileName));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/auth/student/login").build()
        );
        exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);
        exchange.getAttributes().put(RequestIdFilter.EXCHANGE_ATTRIBUTE, "test-request-id");
        return exchange;
    }
}
