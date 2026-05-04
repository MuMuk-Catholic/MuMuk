package com.mumuk.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RedisConfig {
    /*
    RedisConfig를 사용하지 않고 레디스 기능을 구현시, 데이터가 redis에 저장되는 직렬화 방식을 사용할 수 없음
    가령 최근 검색어를 redis 서버에 저장시, Config가 없을 경우 바이너리 코드로 redis에 저장되기 때문에
    직렬화 방식을 선택할 수 있게 하고자 redisConfig 사용
     */

    @Bean
    // 검색어+검색시간, 검색어+검색 빈도 저장시 사용
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        // 검색시간, 검색 빈도(score) 가 오기 때문에 object 사용함

        template.setConnectionFactory(factory); // redis 서버와 연결을 담당하는 ConnectionFactory 생성

        // 키가 redis에 저장될 때 문자열로 저장하게 하는 역할
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

        // Json 직렬화기 생성, 밸류가 redis에 저장될 때 json으로 저장하게 하는 역할
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();

        // key들에 문자열 직렬화기 사용
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // value에 json 직렬화기 사용
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);

        return template;

    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
