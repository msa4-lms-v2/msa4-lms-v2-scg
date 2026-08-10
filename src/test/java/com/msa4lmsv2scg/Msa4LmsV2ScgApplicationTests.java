package com.msa4lmsv2scg;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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
        "PAYMENT_SERVICE_PREDICATE=/api/payments/**",
        "PAYMENT_SERVICE_OPEN_API_PATH=/api-docs"
})
class Msa4LmsV2ScgApplicationTests {

    @Test
    void contextLoads() {
    }

}
