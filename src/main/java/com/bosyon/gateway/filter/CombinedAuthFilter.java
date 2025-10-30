package com.bosyon.gateway.filter;

import com.bosyon.gateway.config.FilterOrderConfig;
import com.bosyon.gateway.model.AuthContext;
import com.bosyon.gateway.model.JwtBaseInfo;
import com.bosyon.gateway.model.TenantInfo;
import com.bosyon.gateway.services.TenantPublicKeyService;
import com.bosyon.gateway.services.TokenBlacklistService;
import com.bosyon.gateway.utils.JwtTokenUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;
import jakarta.validation.ValidationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对请求进行过滤校验
 *
 * 1. 白名单放行：登录、注册等接口跳过验证
 * 2. JWT 解析：提取 kid、jti、tenant_id、exp 等
 * 3. 租户识别： 通过 kid 获取租户，验证一致性
 * 4. 过期检查： 快速检查 exp，避免无效验签
 * 5. 签名验证: 使用对应公钥验证 JWT
 * 6. 黑名单检查: 按路径策略检查 jti 是否在黑名单
 * 7. 上下文设置: 设置认证信息供下游使用
 * 8. 异常处理: 统一返回 401，防信息泄露
 *
 */
@Component
public class CombinedAuthFilter implements GlobalFilter, Ordered {



    // JWT 相关常量
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final int JWT_SEGMENT_COUNT = 3;

    // 白名单路径配置
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/public/**",
            "/v2/api-docs",
            "/v3/api-docs",
            "/swagger-resources/**",
            "/swagger-ui/**",
            "/webjars/**"
    );


    // 重要的请求需要检查token是否被撤销
    private static final List<String> BLACKLIST_CHECK_PATHS = Arrays.asList(
            "/api/**",
            "/admin/**"
    );


    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;


    private final PathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 租户公钥服务（需要注入或实现）
    @Autowired
    private TenantPublicKeyService tenantPublicKeyService;

    @Autowired
    private TokenBlacklistService blacklistService;


    @Override
    public int getOrder() {
        return FilterOrderConfig.BLACKLIST_FILTER_ORDER;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        // 1. 白名单路径放行
        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        // 2. 获取并检查 Authorization 头
        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (StringUtils.isEmpty(authHeader) || !authHeader.startsWith(BEARER_PREFIX)) {
            return createUnauthorizedResponse(exchange, "Missing or invalid Authorization header");
        }

        // 提取 JWT token
        String jwtToken = authHeader.substring(BEARER_PREFIX.length());

        try {
            // 3. JWT 格式检查
            if (!isValidJwtFormat(jwtToken)) {
                return createUnauthorizedResponse(exchange, "Invalid JWT format");
            }

            // 4. 基础解析
            JwtBaseInfo jwtBaseInfo = parseJwtBaseInfo(jwtToken);
            if (jwtBaseInfo == null) {
                return createUnauthorizedResponse(exchange, "Failed to parse JWT");
            }

            // 5. 过期检查
            if (isTokenExpired(jwtBaseInfo.getExp())) {
                return createUnauthorizedResponse(exchange, "Token expired");
            }

            // 6. 租户识别
            TenantInfo tenantInfo = identifyTenant(jwtBaseInfo.getKid());
            if (tenantInfo == null) {
                return createUnauthorizedResponse(exchange, "Invalid tenant");
            }

            // 验证租户一致性
            if (!isTenantConsistent(jwtBaseInfo.getTenantId(), tenantInfo)) {
                return createUnauthorizedResponse(exchange, "Tenant mismatch");
            }

            // 7. 签名验证
            if (!verifySignature(jwtToken, tenantInfo.getPublicKey())) {
                return createUnauthorizedResponse(exchange, "Invalid signature");
            }

            // 8. 黑名单检查（按路径策略）
            if (shouldCheckBlacklist(path) && isTokenBlacklisted(jwtBaseInfo.getJti())) {
                return createUnauthorizedResponse(exchange, "Token revoked");
            }

            // 9. 上下文设置
            return chain.filter(setAuthenticationContext(exchange, jwtBaseInfo, tenantInfo));

        } catch (Exception e) {
            // 记录详细错误日志
            logger.error("Authentication error: " + e.getMessage());
            return createUnauthorizedResponse(exchange, "Authentication failed");
        }
    }

    private boolean isPublicPath(String path) {
        return WHITE_LIST.stream()
                .anyMatch(whitePath -> pathMatcher.match(whitePath, path));
    }

    /**
     * 检查 JWT 格式是否有效
     */
    private boolean isValidJwtFormat(String jwtToken) {
        if (StringUtils.isBlank(jwtToken)) {
            return false;
        }

        // 检查是否是标准的三段式结构
        String[] segments = jwtToken.split("\\.");
        if (segments.length != JWT_SEGMENT_COUNT) {
            return false;
        }

        // 检查每段是否都是有效的 Base64URL
        for (String segment : segments) {
            if (!isValidBase64Url(segment)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 解析 JWT 基础信息（不验证签名）
     */
    private JwtBaseInfo parseJwtBaseInfo(String jwtToken) {
        try {
            String[] segments = jwtToken.split("\\.");
            String headerSegment = segments[0];
            String payloadSegment = segments[1];

            // Base64URL 解码
            byte[] headerBytes = Base64.getUrlDecoder().decode(headerSegment);
            byte[] payloadBytes = Base64.getUrlDecoder().decode(payloadSegment);

            // JSON 解析
            JsonNode headerNode = objectMapper.readTree(headerBytes);
            JsonNode payloadNode = objectMapper.readTree(payloadBytes);

            // 提取关键字段
            JwtBaseInfo info = new JwtBaseInfo();

            // 头部字段
            if (headerNode.has("kid")) {
                info.setKid(headerNode.get("kid").asText());
            }
            if (headerNode.has("alg")) {
                info.setAlg(headerNode.get("alg").asText());
            }
            if (headerNode.has("typ")) {
                info.setTyp(headerNode.get("typ").asText());
            }

            // 载荷字段
            if (payloadNode.has("jti")) {
                info.setJti(payloadNode.get("jti").asText());
            }
            if (payloadNode.has("exp")) {
                info.setExp(payloadNode.get("exp").asLong());
            }
            if (payloadNode.has("tenant_id")) {
                info.setTenantId(payloadNode.get("tenant_id").asText());
            }
            if (payloadNode.has("sub")) {
                info.setSub(payloadNode.get("sub").asText());
            }
            if (payloadNode.has("iss")) {
                info.setIss(payloadNode.get("iss").asText());
            }
            if (payloadNode.has("user_id")) {
                info.setUserId(payloadNode.get("user_id").asText());
            }
            if (payloadNode.has("roles")) {
                List<String> roles = new ArrayList<>();
                payloadNode.get("roles").forEach(role -> roles.add(role.asText()));
                info.setRoles(roles);
            }

            info.setRawToken(jwtToken);
            return info;

        } catch (Exception e) {
            logger.error("JWT parsing error: " + e.getMessage());
            return null;
        }
    }

    /**
     * 检查字符串是否为有效的 Base64URL 编码
     */
    private boolean isValidBase64Url(String str) {
        if (StringUtils.isEmpty(str)) {
            return false;
        }

        // Base64URL 字符集: A-Z a-z 0-9 - _
        String base64UrlPattern = "^[A-Za-z0-9_-]+$";
        if (!str.matches(base64UrlPattern)) {
            return false;
        }

        // 检查长度是否为4的倍数（Base64编码特征）
        return str.length() % 4 == 0;
    }

    /**
     * 检查令牌是否过期
     */
    private boolean isTokenExpired(Long exp) {
        if (exp == null) {
            return true; // 没有过期时间视为过期
        }

        long currentTime = System.currentTimeMillis() / 1000;
        return exp < currentTime;
    }

    /**
     * 通过 kid 识别租户
     */
    private TenantInfo identifyTenant(String kid) {
        if (StringUtils.isEmpty(kid)) {
            return null;
        }

        try {
            // 从租户服务获取公钥信息
            return tenantPublicKeyService.getTenantByKid(kid);
        } catch (Exception e) {
            System.err.println("Tenant identification error: " + e.getMessage());
            return null;
        }
    }

    /**
     * 验证租户一致性
     */
    private boolean isTenantConsistent(String tokenTenantId, TenantInfo tenantInfo) {
        if (StringUtils.isEmpty(tokenTenantId)) {
            return false;
        }

        // 比较令牌中的租户ID与kid对应的租户ID是否一致
        return tokenTenantId.equals(tenantInfo.getTenantId());
    }

    /**
     * 验证 JWT 签名
     */
    private boolean verifySignature(String jwtToken, String publicKey) {
        try {
            // TODO 验证 JWT 签名
            // 这里使用具体的JWT验证库，例如jjwt
            // 以下为伪代码，需要根据实际使用的库进行调整
            Jwts.parser()
                    .setSigningKey(publicKey)
                    .parseClaimsJws(jwtToken);
            return true;
        } catch (Exception e) {
            System.err.println("Signature verification failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检查是否需要进行黑名单检查
     */
    private boolean shouldCheckBlacklist(String path) {
        return BLACKLIST_CHECK_PATHS.stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    /**
     * 检查令牌是否在黑名单中
     */
    private boolean isTokenBlacklisted(String jti) {
        if (StringUtils.isEmpty(jti)) {
            return false;
        }

        try {
            return blacklistService.isTokenRevoked(jti);
        } catch (Exception e) {
            System.err.println("Blacklist check error: " + e.getMessage());
            // 黑名单检查失败时，出于安全考虑，拒绝访问
            return true;
        }
    }

    /**
     * 设置认证上下文
     */
    private ServerWebExchange setAuthenticationContext(ServerWebExchange exchange,
                                                       JwtBaseInfo jwtInfo,
                                                       TenantInfo tenantInfo) {
        // 创建认证上下文对象
        AuthContext authContext = new AuthContext();
        authContext.setUserId(jwtInfo.getUserId());
        authContext.setTenantId(jwtInfo.getTenantId());
        authContext.setJti(jwtInfo.getJti());
        authContext.setRoles(jwtInfo.getRoles());
        authContext.setAuthenticated(true);

        // 将认证信息添加到请求头，供下游服务使用
        ServerHttpRequest newRequest = exchange.getRequest().mutate()
                .header("X-User-Id", jwtInfo.getUserId())
                .header("X-Tenant-Id", jwtInfo.getTenantId())
                .header("X-Authenticated", "true")
                .header("X-User-Roles", String.join(",", jwtInfo.getRoles()))
                .build();

        // 也可以将完整对象存储到exchange属性中
        return exchange.mutate()
                .request(newRequest)
                .build();
    }


    /**
     * 创建未授权响应
     */
    private Mono<Void> createUnauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // 防止信息泄露，返回统一错误信息
        String responseBody = "{\"code\":401,\"message\":\"Unauthorized ---- " + message + "\"}";
        DataBuffer buffer = response.bufferFactory().wrap(responseBody.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }



}
