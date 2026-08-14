package com.msa4lmsv2scg.global.filter;

import com.msa4lmsv2scg.global.resilience.GatewayResilienceProperties;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Slf4j
@Component
public class ResilienceFilter implements GlobalFilter, Ordered {

    private static final String PROFILE_METADATA = "resilience-profile";

    private final GatewayResilienceProperties properties;
    private final Map<String, RouteResilience> routeResilience = new ConcurrentHashMap<>();

    public ResilienceFilter(GatewayResilienceProperties properties) {
        this.properties = properties;
    }

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return chain.filter(exchange);
        }

        Object profileValue = route.getMetadata().get(PROFILE_METADATA);
        if (!(profileValue instanceof String profileName) || profileName.isBlank()) {
            return chain.filter(exchange);
        }

        GatewayResilienceProperties.Profile profile = properties.profiles().get(profileName);
        if (profile == null) {
            return Mono.error(new IllegalStateException("정의되지 않은 resilience profile입니다: " + profileName));
        }

        RouteResilience resilience = routeResilience.computeIfAbsent(
                profileName,
                ignored -> create(profileName, profile)
        );
        String requestId = exchange.getAttributeOrDefault(RequestIdFilter.EXCHANGE_ATTRIBUTE, "unknown");

        return chain.filter(exchange)
                .transformDeferred(BulkheadOperator.of(resilience.bulkhead()))
                .transformDeferred(TimeLimiterOperator.of(resilience.timeLimiter()))
                .transformDeferred(CircuitBreakerOperator.of(resilience.circuitBreaker()))
                .doOnError(error -> log.warn(
                        "Gateway resilience rejected or failed request [requestId={}, routeId={}, profile={}, error={}]",
                        requestId,
                        route.getId(),
                        profileName,
                        error.getClass().getSimpleName()
                ));
    }

    private RouteResilience create(String profileName, GatewayResilienceProperties.Profile profile) {
        validate(profileName, profile);

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(profile.failureRateThreshold())
                .slidingWindowSize(profile.slidingWindowSize())
                .minimumNumberOfCalls(profile.minimumNumberOfCalls())
                .waitDurationInOpenState(profile.openStateDuration())
                .permittedNumberOfCallsInHalfOpenState(Math.max(1, profile.minimumNumberOfCalls() / 2))
                .build();
        BulkheadConfig bulkheadConfig = BulkheadConfig.custom()
                .maxConcurrentCalls(profile.maxConcurrentCalls())
                .maxWaitDuration(Duration.ZERO)
                .build();
        TimeLimiterConfig timeLimiterConfig = TimeLimiterConfig.custom()
                .timeoutDuration(profile.timeout())
                .cancelRunningFuture(true)
                .build();

        return new RouteResilience(
                CircuitBreaker.of("gateway-" + profileName, circuitBreakerConfig),
                Bulkhead.of("gateway-" + profileName, bulkheadConfig),
                TimeLimiter.of("gateway-" + profileName, timeLimiterConfig)
        );
    }

    private void validate(String profileName, GatewayResilienceProperties.Profile profile) {
        if (profile.timeout() == null || profile.timeout().isZero() || profile.timeout().isNegative()
                || profile.openStateDuration() == null || profile.openStateDuration().isZero()
                || profile.openStateDuration().isNegative()
                || profile.maxConcurrentCalls() < 1
                || profile.failureRateThreshold() <= 0 || profile.failureRateThreshold() > 100
                || profile.slidingWindowSize() < 1
                || profile.minimumNumberOfCalls() < 1
                || profile.minimumNumberOfCalls() > profile.slidingWindowSize()) {
            throw new IllegalStateException("잘못된 resilience profile 설정입니다: " + profileName);
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private record RouteResilience(
            CircuitBreaker circuitBreaker,
            Bulkhead bulkhead,
            TimeLimiter timeLimiter
    ) {
    }
}
