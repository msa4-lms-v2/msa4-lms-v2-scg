package com.msa4lmsv2scg.global.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(
        String issuer,
        String accessAudience,
        String kid,
        String publicKeyB64,
        String headerKey,
        String scheme,
        List<String> publicPaths,
        List<String> tokenPassThroughPaths
) {
}
