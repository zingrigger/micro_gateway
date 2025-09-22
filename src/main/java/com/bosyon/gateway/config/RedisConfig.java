package com.bosyon.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;


@Configuration
public class RedisConfig {

	@Bean
	public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
		RedisTemplate<String, Object> redisTemplate = new RedisTemplate<String, Object>();
		GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
		StringRedisSerializer stringSerializer = new StringRedisSerializer();
		//key采用String的序列化方式
		redisTemplate.setKeySerializer(stringSerializer);
		//value序列化方式采用jackson
		redisTemplate.setValueSerializer(serializer);
		//hash的key采用String的序列化方式
		redisTemplate.setHashKeySerializer(stringSerializer);
		//hash的value序列化方式采用jackson
		redisTemplate.setHashValueSerializer(serializer);
		//开启事务
		redisTemplate.setEnableTransactionSupport(true);
		redisTemplate.setConnectionFactory(factory);
		return redisTemplate;
	}
}
