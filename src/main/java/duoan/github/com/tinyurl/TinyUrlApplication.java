package duoan.github.com.tinyurl;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.time.Duration;

@SpringBootApplication
@EnableCaching
public class TinyUrlApplication {

    public static void main(String[] args) {
        SpringApplication.run(TinyUrlApplication.class, args);
    }


    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        return new JedisConnectionFactory();
    }

    @Bean
    RedissonClient redissonClient() {
        return Redisson.create();
    }

    @Bean
    CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // Local Cache with Caffeine
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(60)));
        caffeineCacheManager.setAllowNullValues(false);

        // Distributed Cache with Redis
        RedisCacheManager redisCacheManager = RedisCacheManager.builder(redisConnectionFactory)
                // Expiration time for Redis cache
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(7)))
                .build();

        CompositeCacheManager compositeCacheManager = new CompositeCacheManager(caffeineCacheManager, redisCacheManager);
        compositeCacheManager.setFallbackToNoOpCache(true);

        return compositeCacheManager;
    }
}
