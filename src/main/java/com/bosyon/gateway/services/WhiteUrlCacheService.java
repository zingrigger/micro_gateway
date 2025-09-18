package com.bosyon.gateway.services;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class WhiteUrlCacheService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String WHITE_LIST_KEY = "jwt:white-list";

    @PostConstruct
    public void initWhiteList() {
        // 初始化白名单 URL
        List<String> whiteList = List.of(
                "/usercenter/login/in"
        );
        for (String url : whiteList) {
            redisTemplate.opsForSet().add(WHITE_LIST_KEY, url);
        }
    }

    public Set<String> getWhiteList() {
        return redisTemplate.opsForSet().members(WHITE_LIST_KEY);
    }

}
