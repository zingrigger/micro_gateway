package com.bosyon.gateway.services.impl;

import com.bosyon.gateway.services.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * TODO 基于 redis 的令牌黑名单
 */
@Service
public class RedisTokenBlacklistServiceImpl implements TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(RedisTokenBlacklistServiceImpl.class);

    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";
    private static final String USER_TOKENS_KEY_PREFIX = "jwt:user:tokens:";

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final JwtParser jwtParser;

    public RedisTokenBlacklistServiceImpl(ReactiveRedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.jwtParser = Jwts.parser()
                .setSigningKeyResolver(new SigningKeyResolverAdapter() {
                    // 临时解析器，仅用于获取声明
                    @Override
                    public Key resolveSigningKey(JwsHeader header, Claims claims) {
                        return null;
                    }
                });
    }
    @Override
    public Mono<Boolean> blacklistToken(String token, Duration ttl) {
        try {
            // 解析JWT获取信息（不验证签名）
            String untrustedToken = token.substring(7); // 去掉 "Bearer "
            Claims claims = parseTokenWithoutValidation(untrustedToken);

            String jti = claims.getId();
            String userId = claims.getSubject();
            Date expiration = claims.getExpiration();

            // 计算剩余有效时间
            Duration remainingTtl = Duration.between(Instant.now(), expiration.toInstant());
            if (remainingTtl.isNegative()) {
                return Mono.just(false); // 令牌已过期，无需加入黑名单
            }

            // 使用 jti 作为黑名单键
            String blacklistKey = BLACKLIST_KEY_PREFIX + jti;
            String userTokensKey = USER_TOKENS_KEY_PREFIX + userId;

            return redisTemplate.opsForValue()
                    .set(blacklistKey, "blacklisted", remainingTtl)
                    .flatMap(success -> {
                        if (success) {
                            // 将令牌ID关联到用户
                            return redisTemplate.opsForSet()
                                    .add(userTokensKey, jti)
                                    .thenReturn(true);
                        }
                        return Mono.just(false);
                    })
                    .doOnSuccess(result -> {
                        if (result) {
                            logger.info("Token blacklisted - JTI: {}, User: {}, TTL: {}s",
                                    jti, userId, remainingTtl.getSeconds());
                        }
                    });

        } catch (Exception e) {
            logger.error("Failed to blacklist token", e);
            return Mono.just(false);
        }
    }

    @Override
    public Mono<Boolean> isTokenBlacklisted(String tenantId, String token) {
        try {
            String untrustedToken = token.startsWith("Bearer ") ?
                    token.substring(7) : token;
            Claims claims = parseTokenWithoutValidation(untrustedToken);
            String jti = claims.getId();
            String blacklistKey = BLACKLIST_KEY_PREFIX + jti;

            return redisTemplate.hasKey(blacklistKey);

        } catch (Exception e) {
            logger.error("Failed to check token blacklist status", e);
            return Mono.just(true); // 解析失败时默认视为黑名单
        }
    }

    @Override
    public Mono<Boolean> blacklistAllUserTokens(String userId) {
        String userTokensKey = USER_TOKENS_KEY_PREFIX + userId;

        return redisTemplate.opsForSet()
                .members(userTokensKey)
                .flatMap(jti -> {
                    String blacklistKey = BLACKLIST_KEY_PREFIX + jti;
                    return redisTemplate.opsForValue()
                            .set(blacklistKey, "blacklisted", Duration.ofSeconds(1));
                })
                .then(redisTemplate.delete(userTokensKey))
                .map(count -> count > 0)
                .doOnSuccess(result -> {
                    if (result) {
                        logger.info("All tokens blacklisted for user: {}", userId);
                    }
                });
    }

    @Override
    public Mono<Long> cleanupExpiredTokens() {
        // Redis会自动清理过期的key，这里可以添加额外的清理逻辑
        return Mono.just(0L);
    }

    private Claims parseTokenWithoutValidation(String token) {
        // 解析JWT但不验证签名，仅用于获取声明信息
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new MalformedJwtException("Invalid JWT format");
        }

        String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
        return Jwts.parser()
                .parseClaimsJwt(token + ".signature") // 添加伪签名
                .getBody();
    }
}
