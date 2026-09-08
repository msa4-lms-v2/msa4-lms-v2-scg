package com.msa4lmsv2scg.global.websocket;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import com.msa4lmsv2scg.global.jwt.JwtProvider;
import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Mono;

class WebSocketTicketControllerTest {
    @Test
    void localControllerRejectsMissingJwtWithoutIssuingTicket() {
        var jwt = mock(JwtProvider.class);
        var tickets = mock(WebSocketTicketService.class);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/notifications/ws-ticket"));
        when(jwt.extractAccessToken(exchange)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> new WebSocketTicketController(jwt, tickets).issue(exchange))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(tickets);
    }
    @Test
    void usesVerifiedClaimsRatherThanClientHeaders() {
        var jwt = mock(JwtProvider.class);
        var tickets = mock(WebSocketTicketService.class);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/api/notifications/ws-ticket")
                .header("X-User-Id", "999").header("X-User-Role", "ADMIN"));
        Instant expiry = Instant.now().plusSeconds(60);
        var claims = Jwts.claims().subject("42").add("role", "STUDENT").expiration(Date.from(expiry)).build();
        when(jwt.extractAccessToken(exchange)).thenReturn(Optional.of("jwt"));
        when(jwt.extractClaims("jwt")).thenReturn(claims);
        when(tickets.issue("42", "STUDENT", claims.getExpiration().toInstant())).thenReturn(Mono.just("ticket"));
        var response = new WebSocketTicketController(jwt, tickets).issue(exchange).block();
        assertThat(response.data().get("ticket")).isEqualTo("ticket");
        assertThat(exchange.getResponse().getHeaders().getCacheControl()).isEqualTo("no-store");
    }
}
