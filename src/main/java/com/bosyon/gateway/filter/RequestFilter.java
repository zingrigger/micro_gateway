package com.bosyon.gateway.filter;

import com.bosyon.gateway.config.WhiteUrlConfig;
import com.bosyon.gateway.utils.JwtTokenUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Set;

/**
 * 全局过滤器
 * 1. option http
 * 2. get post token
 */
@Component
public class RequestFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(RequestFilter.class);

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private WhiteUrlConfig whiteUrlConfig;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        logger.info("3. RequestFilter 检查请求中的token是否有效");
        ServerHttpRequest request = exchange.getRequest();
        // 1. 检查是否在白名单中
        if (isWhiteListed(request.getPath().value())) {
            // 不过滤
            return chain.filter(exchange);
        }

        // 2. 响应 options 请求 , 返回 204 -> 允许跨域请求
        if (exchange.getRequest().getMethod() == HttpMethod.OPTIONS) {
            ServerHttpResponse response = exchange.getResponse();
            response.setStatusCode(HttpStatus.NO_CONTENT);
            // 直接返回结果，不会执行后续的 filter
            return response.setComplete();
        }


        // 3. 处理需要认证的接口
        // 验证 Authorization token
        if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return onError(exchange, "No Authorization header", HttpStatus.UNAUTHORIZED);
        }
        final String token = request.getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION).get(0);
        if (token == null || !token.startsWith("Bearer ")) {
            return onError(exchange, "Invalid Authorization header", HttpStatus.UNAUTHORIZED);
        }
        // 去掉前面的 [Bearer ]
        final String jwt = token.substring(7);
        Claims claims = jwtTokenUtil.getClaimsFromToken(jwt);
        if (claims == null || claims.getExpiration().before(new Date())) {
            return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }

        // 4. 从 claims 解析参数放到 request 里
        ServerHttpRequest modifiedRequest = getUserContenxtInfoFromClaims(request, claims);
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private ServerHttpRequest getUserContenxtInfoFromClaims(ServerHttpRequest request, Claims claims) {

        String userId = claims.get("userId", String.class);
        String userName = null;
        String orgName = null;
        try {
            userName = URLEncoder.encode(claims.get("name", String.class), "UTF-8");
            orgName = URLEncoder.encode(claims.get("orgName", String.class), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        String orgId = claims.get("orgId", String.class);

        String orgType = claims.get("orgType", String.class);
        String thirdId = claims.get("thirdId", String.class);
        String projectId = claims.get("projectId", String.class);

        String account = claims.get("account", String.class);
        String sfznbm = claims.get("sfznbm", String.class);
        String directlyOrgId = claims.get("directlyOrgId", String.class);
        String tenantCode = claims.get("tenantCode", String.class);
        String dbId = claims.get("dbId", String.class);


        return request.mutate()
                .header("x-user-id", userId)
                .header("x-user-name", userName)
                .header("x-org-id", orgId)
                .header("x-org-name", orgName)
                .header("x-org-type", orgType)
                .header("x-third-id", thirdId)
                .header("x-project-id", projectId)
                .header("x-account", account)
                .header("x-sfznbm", sfznbm)
                .header("x-directly-org-id", directlyOrgId)
                .header("x-tenant-code", tenantCode)
                .header("x-tenant-dbId", dbId)
                .build();
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    private boolean isWhiteListed(String path) {
        Set<String> whiteList = whiteUrlConfig.getWhiteList();
        for (String pattern : whiteList) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getOrder() {
        return -2;
    }


}
