package com.msa4lmsv2scg;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        // Tests must not depend on a developer machine's JWT_SECRET environment variable.
        "jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY",
        "APP_PORT=0",
        "CORS_ALLOW_ORIGIN=http://localhost:3000",
        "AUTH_SERVICE_NAME=auth",
        "AUTH_SERVICE_URI=http://localhost:8081",
        "AUTH_SERVICE_PREDICATE=/api/auth/**",
        "AUTH_SERVICE_OPEN_API_PATH=/api-docs",
        "ACADEMIC_SERVICE_NAME=academic",
        "ACADEMIC_SERVICE_URI=http://localhost:8082",
        "ACADEMIC_SERVICE_PREDICATE=/api/academic/**",
        "ACADEMIC_SERVICE_OPEN_API_PATH=/api-docs",
        "PAYMENT_SERVICE_NAME=payment",
        "PAYMENT_SERVICE_URI=http://localhost:8083",
        "PAYMENT_SERVICE_PREDICATE=/api/payment/**",
        "PAYMENT_SERVICE_OPEN_API_PATH=/api-docs"
})
class Msa4LmsV2ScgApplicationTests {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void contextLoads() {
    }

    @Test
    void payment_라우트는_단수형_prefix만_사용한다() {
        StepVerifier.create(routeDefinitionLocator.getRouteDefinitions()
                        .filter(route -> route.getId().equals("payment"))
                        .single())
                .assertNext(route -> assertThat(route.getPredicates().getFirst().getArgs())
                        .containsValue("/api/payment/**"))
                .verifyComplete();
    }

}
