package duoan.github.com.tinyurl;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
class TinyUrlCache {
    private static final String SHORT_URL_TO_LONG_URL = "SHORT_URL_TO_LONG_URL";
    private static final String LONG_URL_TO_SHORT_URL = "LONG_URL_TO_SHORT_URL";

    private final CacheManager cacheManager;

    TinyUrlCache(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    Optional<String> getLongUrl(String shortUrl) {
        return Optional.ofNullable(cacheManager.getCache(SHORT_URL_TO_LONG_URL))
                .flatMap(cache -> Optional.ofNullable(cache.get(shortUrl, String.class)));
    }

    Optional<String> getShortUrl(String longUrl) {
        return Optional.ofNullable(cacheManager.getCache(LONG_URL_TO_SHORT_URL))
                .flatMap(cache -> Optional.ofNullable(cache.get(longUrl, String.class)));
    }

    void addTinyUrl(String shortUrl, String longUrl) {
        Optional.ofNullable(cacheManager.getCache(SHORT_URL_TO_LONG_URL))
                .ifPresent(x -> x.put(shortUrl, longUrl));
        Optional.ofNullable(cacheManager.getCache(LONG_URL_TO_SHORT_URL))
                .ifPresent(x -> x.put(longUrl, shortUrl));
    }

}
