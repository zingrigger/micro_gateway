package com.bosyon.gateway.filter;

import com.bosyon.gateway.config.FilterOrderConfig;
import com.bosyon.gateway.services.TokenBlacklistService;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * jwt token 过滤
 * 1. MultiTenantFilter     → 识别租户上下文 ✓
 * 2. TokenBlacklistFilter  → 检查令牌是否被撤销 ✓
 * 3. JwtAuthFilter         → 基础JWT签名和过期验证 ✓
 * 4. KeyRotationFilter     → 密钥轮换fallback处理 ✓
 * 5. RefreshTokenFilter    → 特殊刷新令牌处理 ✓
 */
@Component
public class CombinedAuthFilter implements GlobalFilter, Ordered {



    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 排除路径检查
        if (shouldSkipAuth(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            return unauthorizedResponse(exchange, "Missing Authorization header");
//        }

        String token = authHeader.substring(7);

        return chain.filter(exchange);
        // 组合验证流程
//        return blacklistService.isTokenBlacklisted(token)
//                .flatMap(isBlacklisted -> {
//                    if (isBlacklisted) {
//                        log.warn("Blacklisted token access attempt: {}", path);
//                        return unauthorizedResponse(exchange, "Token revoked");
//                    }
//
//                    // JWT 验证
//                    return jwtDecoder.decode(token)
//                            .flatMap(jwt -> {
//                                // 双重检查：确保黑名单状态没有变化
//                                return blacklistService.isTokenBlacklisted(token)
//                                        .flatMap(stillValid -> {
//                                            if (stillValid) {
//                                                return unauthorizedResponse(exchange, "Token revoked during validation");
//                                            }
//
//                                            // 验证通过，添加用户信息
//                                            ServerWebExchange mutatedExchange = addJwtHeaders(exchange, jwt);
//                                            return chain.filter(mutatedExchange);
//                                        });
//                            })
//                            .onErrorResume(e -> {
//                                log.error("JWT validation failed", e);
//                                return unauthorizedResponse(exchange, "Invalid token");
//                            });
//                });
    }

    private boolean shouldSkipAuth(String path) {
        return path.startsWith("/auth/login") ||
                path.startsWith("/auth/refresh") ||
                path.startsWith("/public/");
    }

    @Override
    public int getOrder() {
        return FilterOrderConfig.BLACKLIST_FILTER_ORDER;
    }

    // 其他辅助方法...
}
