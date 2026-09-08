package com.msa4lmsv2scg.global.filter;

import com.msa4lmsv2scg.global.response.GlobalResponseDTO;
import com.msa4lmsv2scg.global.response.constant.CustomResponseCode;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class GradeQueryAuthorizationFilter implements GlobalFilter, Ordered {

    private static final String MY_GRADES_PATH = "/api/academic/grades/me";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String STUDENT_ROLE = "STUDENT";

    private final ObjectMapper objectMapper;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        if (!isMyGradesRequest(exchange)) {
            return chain.filter(exchange);
        }

        String role = exchange.getRequest().getHeaders().getFirst(USER_ROLE_HEADER);
        if (!STUDENT_ROLE.equals(role)) {
            return forbidden(exchange);
        }

        return chain.filter(exchange);
    }

    private boolean isMyGradesRequest(ServerWebExchange exchange) {
        return exchange.getRequest().getMethod() == HttpMethod.GET
                && MY_GRADES_PATH.equals(exchange.getRequest().getPath().value());
    }

    private Mono<Void> forbidden(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(CustomResponseCode.FORBIDDEN_ERROR.getHttpStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] body = objectMapper.writeValueAsBytes(
                GlobalResponseDTO.from(CustomResponseCode.FORBIDDEN_ERROR)
        );
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        // AuthFilter(-2)가 JWT에서 사용자 역할을 주입한 다음 권한을 판정한다.
        return 0;
    }
}
