package com.msa4lmsv2scg.global.websocket;

import com.msa4lmsv2scg.global.jwt.JwtProvider;
import com.msa4lmsv2scg.global.response.GlobalResponseDTO;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "${CORS_ALLOW_ORIGIN}", allowCredentials = "true")
public class WebSocketTicketController {
    private final JwtProvider jwtProvider;
    private final WebSocketTicketService tickets;

    @PostMapping("/api/notifications/ws-ticket")
    public Mono<GlobalResponseDTO<Map<String, String>>> issue(ServerWebExchange exchange) {
        // Local SCG controllers do not pass through Gateway GlobalFilter; verify explicitly.
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        try {
            var token = jwtProvider.extractAccessToken(exchange).orElseThrow();
            var claims = jwtProvider.extractClaims(token);
            if (claims.getExpiration() == null) throw new IllegalArgumentException();
            return tickets.issue(claims.getSubject(), claims.get("role", String.class), claims.getExpiration().toInstant())
                    .map(ticket -> GlobalResponseDTO.SCG_SUCCESS(Map.of("ticket", ticket)));
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}
