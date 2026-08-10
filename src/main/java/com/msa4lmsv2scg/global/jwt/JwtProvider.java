package com.msa4lmsv2scg.global.jwt;

import com.msa4lmsv2scg.global.error.custom.InvalidTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import javax.crypto.SecretKey;
import java.util.Optional;

@Component
public class JwtProvider {
    private final SecretKey secretKey;
    private final JwtConfig jwtConfig;

    public JwtProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        // Auth service uses Base64 URL encoding for the same signing key.
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(jwtConfig.secret()));
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
        // 토큰을 검증하고 claims 추출
        try{
            return Jwts.parser()
                    .verifyWith(this.secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    ;
        } catch (ExpiredJwtException e){
            throw new InvalidTokenException("토큰이 만료됐습니다.");
        } catch (UnsupportedJwtException e) {
            throw new InvalidTokenException("서명이 위조된 토큰입니다.");
        } catch (MalformedJwtException e) {
            throw new InvalidTokenException("토큰 형식이 올바르지 않습니다.");
        } catch (JwtException | IllegalArgumentException e) {
            throw new InvalidTokenException("토큰 검증에 실패했습니다.");
        }
    }
}
