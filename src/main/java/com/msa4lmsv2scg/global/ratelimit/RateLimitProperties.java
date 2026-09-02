package com.msa4lmsv2scg.global.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "scg.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        Policy auth,
        Policy general
) {
    public record Policy(long requests, Duration window) {
    }
}
