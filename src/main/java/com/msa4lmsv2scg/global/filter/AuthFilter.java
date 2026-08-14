package com.msa4lmsv2scg.global.filter;

import com.msa4lmsv2scg.global.jwt.JwtConfig;
import com.msa4lmsv2scg.global.jwt.JwtProvider;
import com.msa4lmsv2scg.global.response.GlobalResponseDTO;
import com.msa4lmsv2scg.global.response.constant.CustomResponseCode;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.PathContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final PathPatternParser PATH_PATTERN_PARSER = new PathPatternParser();

    private final JwtProvider jwtProvider;
    private final JwtConfig jwtConfig;
    private final ObjectMapper objectMapper;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {

        try{
            ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove(jwtConfig.headerKey());
                        headers.remove(USER_ID_HEADER);
                        headers.remove(USER_ROLE_HEADER);
                    })
                    .build();
            ServerWebExchange sanitizedExchange = exchange.mutate().request(sanitizedRequest).build();

            // HTTP Header에서 Access Token 추출
            Optional<String> optionalToken = jwtProvider.extractAccessToken(exchange);

            // 공개 경로와 CORS preflight 요청만 토큰 없이 허용
            if(optionalToken.isEmpty()) {
                if (isPublicRequest(exchange)) {
                    return chain.filter(sanitizedExchange);
                }
                return this.unAuthorized(exchange);
            }

            // JWT 파싱 및 검증. Access Token이 있는 경우, 클레임 추출, 클레임 헤더 셋팅, 다음 필터 진행
            Claims claims = jwtProvider.extractClaims(optionalToken.get());

            // 하위서비스로 전달할 Http Header 셋팅
            String subject = claims.getSubject();
            String role = claims.get("role", String.class);
            if (subject == null || subject.isBlank() || role == null || role.isBlank()) {
                return this.unAuthorized(exchange);
            }

            ServerHttpRequest serverRequest = sanitizedRequest.mutate()
                    .headers(headers -> {
                        headers.set(USER_ID_HEADER, subject);
                        headers.set(USER_ROLE_HEADER, role);
                    })
                    .build();

            // 다음 필터 호출
            return chain.filter(exchange.mutate().request(serverRequest).build());

        } catch (Exception e) {
            return this.unAuthorized(exchange);
        }
    }

    private boolean isPublicRequest(ServerWebExchange exchange) {
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            return true;
        }

        PathContainer path = exchange.getRequest().getPath().pathWithinApplication();
        return jwtConfig.publicPaths().stream()
                .map(PATH_PATTERN_PARSER::parse)
                .anyMatch(pattern -> pattern.matches(path));
    }

    private Mono<Void> unAuthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(CustomResponseCode.SCG_INVALID_TOKEN_ERROR.getHttpStatus());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes = objectMapper.writeValueAsBytes(GlobalResponseDTO.from(CustomResponseCode.SCG_INVALID_TOKEN_ERROR));
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    /*
     * 필터의 실행 순서 결정
     *   - Gateway의 기본 라우팅(0)보다 먼저 실행되어야 하므로 -1을 설정
     * */
    @Override
    public int getOrder() {
        return -2;
    }
}
