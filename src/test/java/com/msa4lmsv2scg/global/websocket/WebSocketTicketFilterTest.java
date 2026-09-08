package com.msa4lmsv2scg.global.websocket;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import com.msa4lmsv2scg.global.filter.AuthFilter;
import com.msa4lmsv2scg.global.jwt.*;
import reactor.core.publisher.Mono;

class WebSocketTicketFilterTest {
    @Test
    void replacesSpoofedHeadersAndRemovesTicketBeforeForwarding() {
        var tickets = mock(WebSocketTicketService.class);
        when(tickets.consume("test")).thenReturn(Mono.just(new WebSocketTicketService.TicketUser("42", "STUDENT", Instant.now().plusSeconds(60))));
        var config = new JwtConfig("issuer", "audience", "kid", "", "Authorization", "Bearer", List.of(), List.of());
        var filter = new AuthFilter(mock(JwtProvider.class), config, new tools.jackson.databind.ObjectMapper(), tickets);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/ws/notifications?ticket=test")
                .header("Upgrade", "websocket").header("X-User-Id", "999").header("X-User-Role", "ADMIN")
                .header("X-User-Expires-At", "99999999999").header("Authorization", "Bearer spoof"));
        var forwarded = new AtomicReference<ServerWebExchange>();
        filter.filter(exchange, request -> { forwarded.set(request); return Mono.empty(); }).block();
        var request = forwarded.get().getRequest();
        assertThat(request.getHeaders().getFirst("X-User-Id")).isEqualTo("42");
        assertThat(request.getHeaders().getFirst("X-User-Role")).isEqualTo("STUDENT");
        assertThat(request.getHeaders().getFirst("Authorization")).isNull();
        assertThat(request.getURI().getQuery()).isNull();
    }
    @Test
    void missingTicketDoesNotReachAcademic() {
        var tickets = mock(WebSocketTicketService.class);
        when(tickets.consume(null)).thenReturn(Mono.error(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED)));
        var config = new JwtConfig("issuer", "audience", "kid", "", "Authorization", "Bearer", List.of(), List.of());
        var filter = new AuthFilter(mock(JwtProvider.class), config, new tools.jackson.databind.ObjectMapper(), tickets);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/ws/notifications").header("Upgrade", "websocket"));
        filter.filter(exchange, request -> { throw new AssertionError("Must not forward"); }).block();
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
    }
}
