package com.bosyon.gateway.model;

import java.util.ArrayList;
import java.util.List;

public class JwtBaseInfo {

    private String rawToken;
    private String kid;          // 密钥ID
    private String alg;          // 算法
    private String typ;          // 类型
    private String jti;          // JWT ID
    private Long exp;            // 过期时间
    private String tenantId;     // 租户ID
    private String sub;          // 主题
    private String iss;          // 签发者
    private String userId;       // 用户ID
    private List<String> roles = new ArrayList<>(); // 用户角色

    @Override
    public String toString() {
        return String.format("kid=%s,jti=%s,exp=%d,tenant=%s,user=%s",
                kid, jti, exp, tenantId, userId);
    }

    public String getRawToken() {
        return rawToken;
    }

    public void setRawToken(String rawToken) {
        this.rawToken = rawToken;
    }

    public String getKid() {
        return kid;
    }

    public void setKid(String kid) {
        this.kid = kid;
    }

    public String getAlg() {
        return alg;
    }

    public void setAlg(String alg) {
        this.alg = alg;
    }

    public String getTyp() {
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Long getExp() {
        return exp;
    }

    public void setExp(Long exp) {
        this.exp = exp;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }
}
