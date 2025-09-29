package com.bosyon.gateway.filter;

import com.bosyon.gateway.config.FilterOrderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * TODO 多租户过滤器
 * 预期功能：
 * 1. 动态密钥：根据租户标识（如URL、子域）动态加载不同密钥
 * 2. 声明（Claims）区分：在JWT负载中嵌入租户信息（如tenant_id或subtenant字段）
 * 3. 安全性需要考虑集成 Spring Security ，OAuth2 等
 *
 * 用 @ConditionalOnProperty 控制是否创建Bean，期望过滤器在禁用时不存在于过滤器链中
 * 需要注意是否依赖其他Bean，条件注解的加载顺序
 *
 * 后续所有JWT处理都需要基于租户上下文
 * 黑名单、密钥管理等都需要按租户隔离
 * 避免重复的租户识别逻辑
 */
@ConditionalOnProperty(
        value = "bosyon.gateway.filters.multi-tenant.enabled",
        havingValue = "true",
        matchIfMissing = true  // 默认启用
)
@Component
public class MultiTenantFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(MultiTenantFilter.class);

    @Override
    public int getOrder() {
        // 可以通过 @Order 设置，也可以重写 getOrder() 方法，getOrder 方法里可以从yml里拿到配置过来
        return FilterOrderConfig.MULTI_TENANT_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // 从请求中识别租户信息
        String tenantId = extractTenantId(exchange);

        // 将租户信息存储到请求上下文中
        exchange.getAttributes().put("tenantId", tenantId);

        logger.debug("Identified tenant: {} for path: {}", tenantId,
                exchange.getRequest().getPath().value());

        return chain.filter(exchange);
    }



    private String extractTenantId(ServerWebExchange exchange) {
        // 1. 从子域名识别: tenant1.app.com -> tenant1
        String host = exchange.getRequest().getURI().getHost();
        if (host.contains(".")) {
            String subdomain = host.substring(0, host.indexOf('.'));
            if (!subdomain.equals("www") && !subdomain.equals("app")) {
                return subdomain;
            }
        }

        // 2. 从请求头识别
        String tenantHeader = exchange.getRequest().getHeaders().getFirst("X-Tenant-ID");
        if (StringUtils.hasText(tenantHeader)) {
            return tenantHeader;
        }

        // 3. 从路径识别: /api/tenant1/users -> tenant1
        String path = exchange.getRequest().getPath().value();
        if (path.startsWith("/api/")) {
            String[] pathSegments = path.split("/");
            if (pathSegments.length >= 3) {
                return pathSegments[2];
            }
        }

        // 4. 默认租户
        return "default";
    }
}
