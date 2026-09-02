package com.msa4lmsv2scg;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        // 비대칭 JWT 전환(feature/jwt-public-key-verification) 이후 JwtProvider는 PEM 공개키를 base64로
        // 감싼 jwt.public-key-b64를 요구한다. 테스트 전용으로 생성한 RSA 키페어의 공개키만 사용한다.
        "jwt.public-key-b64=LS0tLS1CRUdJTiBQVUJMSUMgS0VZLS0tLS0NCk1JSUJJakFOQmdrcWhraUc5dzBCQVFFRkFBT0NBUThBTUlJQkNnS0NBUUVBbWhXUmMvMGhKVk9wODlUM2craVMNCnlOaml6S3plUnpHR2N1alRrcWd4R3k4VGo5azUwTFNxTjkyWklTYTBHUmJvV1VRcHZVSGc5THdNWGF3WlEzWVQNCk45dlZpN0V2OXVvZ1JGMXJqS2xRQjkzSVAxdVU5TkZKaGR0dWdITmtVam9CZnA1NmVIMUpZU1FtUExyUlJqVkMNClkwL1Y5eXpGTko4NGk2RlF1OVFoRDZKOVNkdkRoeE9tYlQyQks3VUZqY0tZWDFrQUd5ODJ6amhlb01JSzBzcWgNCk9STWRIK0JrZEtJS2NFRlBtZy9iQkl0aUNqQzBXYVlPdnMzMnBPRDRka2lQS3VsVUZpTEFMNGlJalhVNTgwK3ANClRDWk43SFJ2QkpES0pFMG90RHYzS1Z0ZndRQmlmcXIydkdQOWJtMFJzTGdhLzh6bzRyeTJPWk9HeEp4eWtyMHYNClR3SURBUUFCDQotLS0tLUVORCBQVUJMSUMgS0VZLS0tLS0NCg==",
        "jwt.kid=test-kid-1",
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
