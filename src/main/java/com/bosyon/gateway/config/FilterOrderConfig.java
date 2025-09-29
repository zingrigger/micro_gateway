package com.bosyon.gateway.config;

/**
 * 过滤器执行顺序
 * 1. MultiTenantFilter     → 识别租户上下文 ✓
 * 2. TokenBlacklistFilter  → 检查令牌是否被撤销 ✓
 * 3. JwtAuthFilter         → 基础JWT签名和过期验证 ✓
 * 4. KeyRotationFilter     → 密钥轮换fallback处理 ✓
 * 5. RefreshTokenFilter    → 特殊刷新令牌处理 ✓
 * 6. 其他业务过滤器        → 限流、日志等
 *
 * TODO 1~5需要使用 组合过滤器减少调用开销
 */
public class FilterOrderConfig {

    // 执行顺序常量（数值越小优先级越高）
    public static final int MULTI_TENANT_FILTER_ORDER = -200;    // 最高优先级
    public static final int BLACKLIST_FILTER_ORDER = -190;       // 次高优先级
    public static final int JWT_AUTH_FILTER_ORDER = -180;        // JWT基础鉴权
    public static final int KEY_ROTATION_FILTER_ORDER = -170;    // 密钥轮换
    public static final int REFRESH_TOKEN_FILTER_ORDER = -160;   // 刷新令牌处理

}
