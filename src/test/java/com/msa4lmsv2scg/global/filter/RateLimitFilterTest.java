package com.msa4lmsv2scg.global.filter;

import com.msa4lmsv2scg.global.ratelimit.RateLimitProperties;
import com.msa4lmsv2scg.global.ratelimit.RedisRateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;

import java.net.InetSocketAddress;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RateLimitFilterTest {

    private final RateLimitProperties.Policy authPolicy =
            new RateLimitProperties.Policy(10, Duration.ofMinutes(1));
    private final RateLimitProperties.Policy generalPolicy =
            new RateLimitProperties.Policy(120, Duration.ofMinutes(1));
    private final RateLimitProperties properties =
            new RateLimitProperties(true, authPolicy, generalPolicy);

    @Test
    void authRequestUsesStrictPolicy() {
        RedisRateLimitService service = mock(RedisRateLimitService.class);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        MockServerWebExchange exchange = exchange("/api/auth/student/login");
        when(service.isAllowed(eq("auth"), anyString(), eq(authPolicy))).thenReturn(Mono.just(true));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        RateLimitFilter filter = new RateLimitFilter(service, properties, new ObjectMapper());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(service).isAllowed(eq("auth"), eq("127.0.0.1"), eq(authPolicy));
        verify(chain).filter(exchange);
    }

    @Test
    void rejectsRequestWhenLimitIsExceeded() {
        RedisRateLimitService service = mock(RedisRateLimitService.class);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        MockServerWebExchange exchange = exchange("/api/academic/courses");
        when(service.isAllowed(eq("general"), anyString(), eq(generalPolicy))).thenReturn(Mono.just(false));

        RateLimitFilter filter = new RateLimitFilter(service, properties, new ObjectMapper());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain, never()).filter(exchange);
        org.junit.jupiter.api.Assertions.assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
    }

    @Test
    void allowsRequestWhenRedisIsUnavailable() {
        RedisRateLimitService service = mock(RedisRateLimitService.class);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        MockServerWebExchange exchange = exchange("/api/payment/charges");
        when(service.isAllowed(eq("general"), anyString(), eq(generalPolicy)))
                .thenReturn(Mono.error(new IllegalStateException("redis down")));
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        RateLimitFilter filter = new RateLimitFilter(service, properties, new ObjectMapper());

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        verify(chain).filter(exchange);
    }

    private MockServerWebExchange exchange(String path) {
        MockServerHttpRequest request = MockServerHttpRequest.get(path)
                .remoteAddress(new InetSocketAddress("127.0.0.1", 54321))
                .build();
        return MockServerWebExchange.from(request);
    }
}
