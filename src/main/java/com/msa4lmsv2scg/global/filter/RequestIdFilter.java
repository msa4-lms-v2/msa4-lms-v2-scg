package com.msa4lmsv2scg.global.filter;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER_NAME = "X-Request-Id";
    public static final String EXCHANGE_ATTRIBUTE = RequestIdFilter.class.getName() + ".requestId";
    public static final String REACTOR_CONTEXT_KEY = "requestId";
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$"
    );

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        String requestId = normalizeOrCreate(exchange.getRequest().getHeaders().getFirst(HEADER_NAME));
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HEADER_NAME);
                    headers.set(HEADER_NAME, requestId);
                })
                .build();
        ServerWebExchange tracedExchange = exchange.mutate().request(request).build();
        tracedExchange.getAttributes().put(EXCHANGE_ATTRIBUTE, requestId);
        tracedExchange.getResponse().getHeaders().set(HEADER_NAME, requestId);

        long startedAt = System.nanoTime();
        log.info("Gateway request started [requestId={}, method={}, path={}]",
                requestId, request.getMethod(), request.getPath().value());

        return chain.filter(tracedExchange)
                .doOnSuccess(ignored -> log.info(
                        "Gateway request completed [requestId={}, status={}, elapsedMs={}]",
                        requestId,
                        tracedExchange.getResponse().getStatusCode(),
                        elapsedMillis(startedAt)
                ))
                .doOnError(error -> log.warn(
                        "Gateway request failed [requestId={}, elapsedMs={}, error={}]",
                        requestId,
                        elapsedMillis(startedAt),
                        error.getClass().getSimpleName()
                ))
                .contextWrite(context -> context.put(REACTOR_CONTEXT_KEY, requestId));
    }

    String normalizeOrCreate(String candidate) {
        if (candidate != null && UUID_PATTERN.matcher(candidate).matches()) {
            return UUID.fromString(candidate).toString().toLowerCase(Locale.ROOT);
        }
        return UUID.randomUUID().toString();
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    @Override
    public int getOrder() {
        return -3;
    }
}
