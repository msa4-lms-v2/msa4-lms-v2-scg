package com.msa4lmsv2scg.global.websocket;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

class WebSocketTicketServiceTest {
    @Test
    @SuppressWarnings("unchecked")
    void issuedTicketIsShortLivedAndCanOnlyBeConsumedOnce() {
        var redis = mock(ReactiveStringRedisTemplate.class);
        ReactiveValueOperations<String, String> values = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var storage = new ConcurrentHashMap<String, String>();
        when(values.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenAnswer(call -> {
            Duration ttl = call.getArgument(2);
            assertThat(ttl).isPositive().isLessThanOrEqualTo(Duration.ofSeconds(30));
            return Mono.just(storage.putIfAbsent(call.getArgument(0), call.getArgument(1)) == null);
        });
        when(values.getAndDelete(anyString())).thenAnswer(call -> Mono.defer(() -> Mono.justOrEmpty(storage.remove(call.getArgument(0)))));
        var service = new WebSocketTicketService(redis);
        String ticket = service.issue("42", "STUDENT", Instant.now().plusSeconds(120)).block();
        assertThat(ticket).hasSize(43);
        assertThat(service.consume(ticket).block().id()).isEqualTo("42");
        assertThatThrownBy(() -> service.consume(ticket).block()).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> service.issue("42", "ADMIN", Instant.now().plusSeconds(60))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThatThrownBy(() -> service.issue("42", "STUDENT", Instant.now().minusSeconds(1))).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
