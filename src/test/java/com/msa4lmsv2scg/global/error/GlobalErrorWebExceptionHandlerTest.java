package com.msa4lmsv2scg.global.error;

import com.msa4lmsv2scg.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalErrorWebExceptionHandlerTest {

    private final GlobalErrorWebExceptionHandler handler =
            new GlobalErrorWebExceptionHandler(new ObjectMapper());

    @Test
    void classifiesRouteNotFoundAsDependencyUnavailable() {
        assertThat(handler.classify(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .isEqualTo(CustomResponseCode.SERVICE_UNAVAILABLE_ERROR);
    }

    @Test
    void classifiesNestedConnectionFailureAsDependencyUnavailable() {
        RuntimeException wrapped = new RuntimeException(new ConnectException("connection refused"));

        assertThat(handler.classify(wrapped))
                .isEqualTo(CustomResponseCode.SERVICE_UNAVAILABLE_ERROR);
    }

    @Test
    void classifiesTimeoutSeparately() {
        assertThat(handler.classify(new TimeoutException("route timed out")))
                .isEqualTo(CustomResponseCode.SERVICE_TIMEOUT_ERROR);
    }

    @Test
    void classifiesCircuitBreakerRejectionSeparatelyWithoutResilienceDependency() {
        assertThat(handler.classify(new CallNotPermittedException()))
                .isEqualTo(CustomResponseCode.CIRCUIT_BREAKER_ERROR);
    }

    @Test
    void keepsUnknownErrorsAsSystemError() {
        assertThat(handler.classify(new IllegalStateException("unexpected")))
                .isEqualTo(CustomResponseCode.SYSTEM_ERROR);
    }

    private static final class CallNotPermittedException extends RuntimeException {
    }
}
