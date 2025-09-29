package com.bosyon.gateway.filter;

import com.bosyon.gateway.config.FilterOrderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * TODO 令牌黑名单
 * 1. 缓存存储：将需失效的令牌标识（如jti）或用户标识存入Redis等缓存
 * 2. 校验拦截：在JWT校验逻辑中增加黑名单检查
 */
@Component
public class TokenBlacklistFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistFilter.class);

    @Override
    public int getOrder() {
        // 可以通过 @Order 设置，也可以重写 getOrder() 方法，getOrder 方法里可以从yml里拿到配置过来
        return FilterOrderConfig.BLACKLIST_FILTER_ORDER;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("令牌黑名单 filter");

        // 基于租户的黑名单检查
        return chain.filter(exchange);
    }
}
