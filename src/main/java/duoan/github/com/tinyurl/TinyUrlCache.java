package duoan.github.com.tinyurl;

import io.micrometer.core.annotation.Timed;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.support.CompositeCacheManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Log4j2
@Service
@Timed(value = "tinyurl.timed.cache", percentiles = {0.5, 0.95, 0.99}, description = "Time taken for cache")
class TinyUrlCache {
    private static final String SHORT_URL_TO_LONG_URL = "S2L";
    private static final String LONG_URL_TO_SHORT_URL = "L2S";

    private final CompositeCacheManager tieredCacheManager;

    TinyUrlCache(CompositeCacheManager tieredCacheManager) {
        this.tieredCacheManager = tieredCacheManager;
    }

    Optional<String> getShortUrl(String longUrl) {
        return Optional.ofNullable(tieredCacheManager.getCache(LONG_URL_TO_SHORT_URL))
                .flatMap(cache -> Optional.ofNullable(cache.get(longUrl, String.class)));
    }

    Optional<String> getLongUrl(String shortUrl) {
        return Optional.ofNullable(tieredCacheManager.getCache(SHORT_URL_TO_LONG_URL))
                .flatMap(cache -> Optional.ofNullable(cache.get(shortUrl, String.class)));
    }

    void dualPutUrlMapping(String shortUrl, String longUrl) {
        Optional.ofNullable(tieredCacheManager.getCache(SHORT_URL_TO_LONG_URL))
                .ifPresent(x -> x.put(shortUrl, longUrl));
        Optional.ofNullable(tieredCacheManager.getCache(LONG_URL_TO_SHORT_URL))
                .ifPresent(x -> x.put(longUrl, shortUrl));
    }

}
