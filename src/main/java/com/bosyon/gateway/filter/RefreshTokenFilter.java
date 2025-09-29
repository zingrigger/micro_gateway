package com.bosyon.gateway.filter;

import com.bosyon.gateway.config.FilterOrderConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * TODO 密钥轮换
 *
 * 1. 双密钥过渡：新旧密钥并存，支持两者验证
 * 2. 动态获取：从可信端点（如KMS）动态获取密钥，避免硬编码
 */
@Component
public class RefreshTokenFilter implements GlobalFilter, Ordered {

    @Override
    public int getOrder() {
        return FilterOrderConfig.REFRESH_TOKEN_FILTER_ORDER;
    }


    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange);
    }
}
