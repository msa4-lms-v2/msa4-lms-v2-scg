package com.msa4lmsv2scg.global.filter;

import com.msa4lmsv2scg.global.ratelimit.RateLimitProperties;
import com.msa4lmsv2scg.global.ratelimit.RedisRateLimitService;
import com.msa4lmsv2scg.global.response.GlobalResponseDTO;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;

@Component
@RequiredArgsConstructor
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final String AUTH_PATH_PREFIX = "/api/auth/";

    private final RedisRateLimitService rateLimitService;
    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        if (!properties.enabled() || exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        RateLimitProperties.Policy policy = selectPolicy(exchange);
        String bucket = isAuthRequest(exchange) ? "auth" : "general";
        String clientIdentifier = resolveClientIdentifier(exchange);

        return rateLimitService.isAllowed(bucket, clientIdentifier, policy)
                .flatMap(allowed -> allowed ? chain.filter(exchange) : tooManyRequests(exchange))
                // Redis 장애가 기존 Access JWT 요청의 가용성에 영향을 주지 않도록 fail-open 한다.
                .onErrorResume(error -> chain.filter(exchange));
    }

    private RateLimitProperties.Policy selectPolicy(ServerWebExchange exchange) {
        return isAuthRequest(exchange) ? properties.auth() : properties.general();
    }

    private boolean isAuthRequest(ServerWebExchange exchange) {
        return exchange.getRequest().getPath().value().startsWith(AUTH_PATH_PREFIX);
    }

    private String resolveClientIdentifier(ServerWebExchange exchange) {
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress == null || remoteAddress.getAddress() == null) {
            return "unknown";
        }
        // 신뢰 프록시 목록이 없는 현재 단계에서는 위조 가능한 X-Forwarded-For를 사용하지 않는다.
        return remoteAddress.getAddress().getHostAddress();
    }

    private Mono<Void> tooManyRequests(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = objectMapper.writeValueAsBytes(new GlobalResponseDTO<Void>(
                "E21",
                "요청 허용량을 초과했습니다.",
                null
        ));
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
