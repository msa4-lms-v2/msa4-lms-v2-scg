package com.msa4lmsv2scg.global.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@ConfigurationProperties(prefix = "gateway.resilience")
public record GatewayResilienceProperties(Map<String, Profile> profiles) {

    public record Profile(
            Duration timeout,
            int maxConcurrentCalls,
            float failureRateThreshold,
            int slidingWindowSize,
            int minimumNumberOfCalls,
            Duration openStateDuration
    ) {
    }
}
