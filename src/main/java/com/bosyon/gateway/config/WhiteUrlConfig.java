package com.bosyon.gateway.config;

import com.bosyon.gateway.services.WhiteUrlCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class WhiteUrlConfig {


    @Autowired
    private WhiteUrlCacheService whiteUrlCacheService;

    public Set<String> getWhiteList() {
        return whiteUrlCacheService.getWhiteList();
    }

}
