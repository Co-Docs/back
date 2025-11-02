package backend.cowrite.config;

import backend.cowrite.service.response.UserCacheDto;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@EnableCaching //Spring Boot의 캐싱 설정을 활성화
public class RedisCacheConfig {

    //캐시 매니저는 여러개 만들어 상황에 따라 사용할 수 있다.
    @Bean
    public CacheManager userCacheManager(RedisConnectionFactory connectionFactory) {
        var keySer   = new org.springframework.data.redis.serializer.StringRedisSerializer();
        var valueSer = new org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer<>(UserCacheDto.class);

        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySer))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(valueSer))
                .entryTtl(Duration.ofMinutes(1));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

}
