package duoan.github.com.tinyurl;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.log4j.Log4j2;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.time.Duration;

@Log4j2
@SpringBootApplication
@EnableCaching
@EnableAspectJAutoProxy
public class TinyUrlApplication {

    public static void main(String[] args) {
        SpringApplication.run(TinyUrlApplication.class, args);
    }


    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
        return new JedisConnectionFactory();
    }

    @Bean
    RedissonClient redissonClient(RedisProperties props) {
        Config config = new Config();
        String address = String.format("redis://%s:%s", props.getHost(), props.getPort());
        log.info("Configuring Redis with address: {}", address);
        config.useSingleServer()
                .setAddress(address)
                .setConnectTimeout(10000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        return Redisson.create(config);
    }

    @Bean("l2CacheManager")
    RedisCacheManager l2CacheManager(RedisConnectionFactory redisConnectionFactory) {
        return RedisCacheManager.builder(redisConnectionFactory)
                // Expiration time for Redis cache
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofDays(7)))
                .build();
    }

    @Bean("l1CacheManager")
    CaffeineCacheManager l1CacheManager() {
        // Local Cache with Caffeine
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(60)));
        caffeineCacheManager.setAllowNullValues(false);
        return caffeineCacheManager;
    }

    @Primary
    @Bean("tieredCacheManager")
    CompositeCacheManager tieredCacheManager(
            CaffeineCacheManager caffeineCacheManager,
            RedisCacheManager redisCacheManager) {
        CompositeCacheManager compositeCacheManager =
                new CompositeCacheManager(caffeineCacheManager, redisCacheManager);
        compositeCacheManager.setFallbackToNoOpCache(true);
        return compositeCacheManager;
    }
}
