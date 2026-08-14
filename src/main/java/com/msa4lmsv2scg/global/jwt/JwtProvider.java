package com.msa4lmsv2scg.global.jwt;

import com.msa4lmsv2scg.global.error.custom.InvalidTokenException;
import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

@Component
public class JwtProvider {

    private static final String ACCESS_TOKEN_TYPE = "access";

    private final PublicKey publicKey;
    private final JwtConfig jwtConfig;

    public JwtProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.publicKey = loadPublicKey(jwtConfig.publicKeyB64());
    }

    public Optional<String> extractAccessToken(ServerWebExchange exchange) {
        String bearerToken = exchange.getRequest().getHeaders().getFirst(jwtConfig.headerKey());

        if (bearerToken == null) {
            return Optional.empty();
        }

        String prefix = jwtConfig.scheme() + " ";
        if (!bearerToken.startsWith(prefix)) {
            throw new InvalidTokenException("인증 헤더 형식이 올바르지 않습니다.");
        }

        String token = bearerToken.substring(prefix.length()).trim();
        if (token.isEmpty()) {
            throw new InvalidTokenException("Access Token이 비어 있습니다.");
        }
        return Optional.of(token);
    }

    public Claims extractClaims(String token) {
        // 서명, 만료, 발급자, 대상, 토큰 종류, kid를 모두 검증한다.
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(this.publicKey)
                    .requireIssuer(jwtConfig.issuer())
                    .requireAudience(jwtConfig.accessAudience())
                    .require("token_type", ACCESS_TOKEN_TYPE)
                    .build()
                    .parseSignedClaims(token);

            String kid = jws.getHeader().getKeyId();
            if (kid == null || !kid.equals(jwtConfig.kid())) {
                throw new InvalidTokenException("알 수 없는 서명 키입니다.");
            }
            return jws.getPayload();
        } catch (ExpiredJwtException e) {
            throw new InvalidTokenException("토큰이 만료됐습니다.");
        } catch (UnsupportedJwtException e) {
            throw new InvalidTokenException("서명이 위조된 토큰입니다.");
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("토큰 형식이 올바르지 않습니다.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("토큰 검증에 실패했습니다.");
        }
    }

    private PublicKey loadPublicKey(String base64Pem) {
        try {
            String pem = new String(Base64.getDecoder().decode(base64Pem))
                    .replaceAll("-----BEGIN (.*)-----", "")
                    .replaceAll("-----END (.*)-----", "")
                    .replaceAll("\\s", "");
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pem)));
        } catch (Exception e) {
            throw new IllegalStateException("JWT 공개키를 불러올 수 없습니다.", e);
        }
    }
}
