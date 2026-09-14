package com.msa4lmsv2scg.global.filter;

import com.msa4lmsv2scg.global.jwt.JwtConfig;
import com.msa4lmsv2scg.global.jwt.JwtProvider;
import com.msa4lmsv2scg.global.websocket.WebSocketTicketService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AdmissionInternalRouteTest {
    private final AuthFilter filter = new AuthFilter(mock(JwtProvider.class),
            new JwtConfig("issuer", "audience", "kid", "", "Authorization", "Bearer", List.of(), List.of()),
            new ObjectMapper(), mock(WebSocketTicketService.class));

    @Test
    void internalAdmissionMutationsNeverReachAcademicThroughGateway() {
        for (String action : List.of("bill", "paid", "student", "activated")) {
            var exchange = MockServerWebExchange.from(MockServerHttpRequest
                    .post("/api/academic/internal/admissions/7/" + action)
                    .header("X-Admission-Token", "spoofed").header("X-User-Role", "ADMIN"));
            filter.filter(exchange, request -> { throw new AssertionError("내부 경로는 전달할 수 없습니다."); }).block();
            assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
        }
    }

    @Test
    void clientCannotForwardAdmissionServiceTokenOnPublicPreflight() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.options("/api/academic/example")
                .header("X-Admission-Token", "spoofed"));
        filter.filter(exchange, forwarded -> {
            assertThat(forwarded.getRequest().getHeaders().getFirst("X-Admission-Token")).isNull();
            return Mono.empty();
        }).block();
    }
}
