package com.bosyon.gateway.config;

import com.bosyon.gateway.services.WhiteUrlCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 系统内部接口白名单
 * 用于放行不需要登录验证的接口
 */
@Component
public class WhiteUrlConfig {


    @Autowired
    private WhiteUrlCacheService whiteUrlCacheService;

    public Set<String> getWhiteList() {
        return whiteUrlCacheService.getWhiteList();
    }

}
