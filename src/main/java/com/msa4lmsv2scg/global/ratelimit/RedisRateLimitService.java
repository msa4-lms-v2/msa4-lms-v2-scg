package com.msa4lmsv2scg.global.ratelimit;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisRateLimitService {

    private static final String KEY_PREFIX = "gateway:rate-limit:";
    private static final DefaultRedisScript<Long> INCREMENT_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final Clock clock = Clock.systemUTC();

    public Mono<Boolean> isAllowed(String bucket, String clientIdentifier, RateLimitProperties.Policy policy) {
        validate(policy);
        String key = buildKey(bucket, clientIdentifier, policy.window());

        return redisTemplate.execute(
                        INCREMENT_SCRIPT,
                        List.of(key),
                        Long.toString(policy.window().toMillis())
                )
                .next()
                .map(count -> count <= policy.requests());
    }

    String buildKey(String bucket, String clientIdentifier, Duration window) {
        long windowNumber = clock.millis() / window.toMillis();
        return KEY_PREFIX + bucket + ":" + hash(clientIdentifier) + ":" + windowNumber;
    }

    private void validate(RateLimitProperties.Policy policy) {
        if (policy == null || policy.requests() < 1 || policy.window() == null || policy.window().isZero()
                || policy.window().isNegative()) {
            throw new IllegalStateException("Rate limit 설정은 1회 이상의 요청과 양수 window가 필요합니다.");
        }
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
