package com.msa4lmsv2scg.global.error;

import com.msa4lmsv2scg.global.response.GlobalResponseDTO;
import com.msa4lmsv2scg.global.response.constant.CustomResponseCode;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2) // Spring의 기본 ErrorWebExceptionHandler(-1) 보다 먼저 실행시키기 위해 '-2'를 설정
@RequiredArgsConstructor
public class GlobalErrorWebExceptionHandler implements WebExceptionHandler {
    private final ObjectMapper objectMapper;

    @Override
    @NonNull
    public Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        CustomResponseCode customResponseCode = classify(ex);


        response.setStatusCode(customResponseCode.getHttpStatus()); // Http Status 변경
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON); // Content Type을 JSON으로 변경

        // response body에 담을 데이터
        byte[] bytes = objectMapper.writeValueAsBytes(GlobalResponseDTO.from(customResponseCode));
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    CustomResponseCode classify(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String simpleName = current.getClass().getSimpleName();
            if ("CallNotPermittedException".equals(simpleName)) {
                return CustomResponseCode.CIRCUIT_BREAKER_ERROR;
            }
            if (current instanceof TimeoutException
                    || current instanceof SocketTimeoutException
                    || simpleName.toLowerCase().contains("timeout")) {
                return CustomResponseCode.SERVICE_TIMEOUT_ERROR;
            }
            if (current instanceof ConnectException
                    || current instanceof UnknownHostException
                    || current instanceof NoRouteToHostException) {
                return CustomResponseCode.SERVICE_UNAVAILABLE_ERROR;
            }
            if (current instanceof ResponseStatusException statusException) {
                int status = statusException.getStatusCode().value();
                if (status == HttpStatus.GATEWAY_TIMEOUT.value()) {
                    return CustomResponseCode.SERVICE_TIMEOUT_ERROR;
                }
                if (status == HttpStatus.NOT_FOUND.value()
                        || status == HttpStatus.BAD_GATEWAY.value()
                        || status == HttpStatus.SERVICE_UNAVAILABLE.value()) {
                    return CustomResponseCode.SERVICE_UNAVAILABLE_ERROR;
                }
            }
            current = current.getCause();
        }
        return CustomResponseCode.SYSTEM_ERROR;
    }
}
