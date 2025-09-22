package com.bosyon.gateway.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtTokenUtil {

    @Value("${usercenter.token.secret}")
    private String secret;

    /**
     * 解析 token
     *
     * @param token
     * @return
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
        } catch (SignatureException e) {
            return null;
        }
    }

    /**
     * 检查 token 是否过期
     *
     * @param token
     * @return
     */
    public boolean isTokenExpired(String token) {
        final Claims claims = getClaimsFromToken(token);
        return claims == null || claims.getExpiration().before(new Date());
    }
}
