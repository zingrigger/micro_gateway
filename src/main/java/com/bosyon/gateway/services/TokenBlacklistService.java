package com.bosyon.gateway.services;

import reactor.core.publisher.Mono;

import java.time.Duration;


/**
 * 黑令牌
 */
public interface TokenBlacklistService {

    /**
     * 将令牌加入黑名单
     */
    Mono<Boolean> blacklistToken(String token, Duration ttl);

    /**
     * 检查令牌是否在黑名单中
     */
    Mono<Boolean> isTokenBlacklisted(String tenantId, String token);

    /**
     * 根据用户ID将用户的所有令牌加入黑名单（强制登出）
     */
    Mono<Boolean> blacklistAllUserTokens(String userId);

    /**
     * 清理过期的黑名单条目
     */
    Mono<Long> cleanupExpiredTokens();


    boolean isTokenRevoked(String jti);

}
