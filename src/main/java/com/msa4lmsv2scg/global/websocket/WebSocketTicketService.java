package com.msa4lmsv2scg.global.websocket;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class WebSocketTicketService {
    private static final String PREFIX = "notification:ws-ticket:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final ReactiveStringRedisTemplate redis;

    public Mono<String> issue(String id, String role, Instant expiresAt) {
        validate(id, role, expiresAt);
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String ticket = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.compareTo(Duration.ofSeconds(30)) > 0) ttl = Duration.ofSeconds(30);
        if (ttl.isNegative() || ttl.isZero()) return Mono.error(unauthorized());
        return redis.opsForValue().setIfAbsent(PREFIX + ticket,
                        id + "|" + role + "|" + expiresAt.getEpochSecond(), ttl)
                .flatMap(saved -> Boolean.TRUE.equals(saved) ? Mono.just(ticket) : Mono.error(unauthorized()));
    }

    public Mono<TicketUser> consume(String ticket) {
        if (ticket == null || !ticket.matches("[A-Za-z0-9_-]{43}")) return Mono.error(unauthorized());
        // Redis GETDEL: concurrent handshakes cannot reuse the same ticket.
        return redis.opsForValue().getAndDelete(PREFIX + ticket)
                .switchIfEmpty(Mono.error(unauthorized()))
                .map(value -> {
                    try {
                        String[] parts = value.split("\\|");
                        Instant expiry = Instant.ofEpochSecond(Long.parseLong(parts[2]));
                        validate(parts[0], parts[1], expiry);
                        return new TicketUser(parts[0], parts[1], expiry);
                    } catch (RuntimeException exception) { throw unauthorized(); }
                });
    }
    private static void validate(String id, String role, Instant expiry) {
        if (id == null || !id.matches("[1-9][0-9]*") || role == null
                || !Set.of("STUDENT", "PROFESSOR").contains(role) || expiry == null || !expiry.isAfter(Instant.now())) {
            throw unauthorized();
        }
    }
    private static ResponseStatusException unauthorized() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED); }
    public record TicketUser(String id, String role, Instant expiresAt) {}
}
