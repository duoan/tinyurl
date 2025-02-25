package duoan.github.com.tinyurl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
@Service
class TinyUrlService {
    /// After {@linkplain #COLLISON_LIMIT} tries of re-salting long urls to avoid collisions, give up.
    private static final int COLLISON_LIMIT = 100;
    private static final String NULL_LONG_URL = "NULL";
    private static final String METRIC_PREFIX = "tinyurl.service";

    private final UrlMappingRepository repository;
    private final TinyUrlCache cache;
    private final MeterRegistry meterRegistry;

    // ThreadLocal for counters
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    TinyUrlService(UrlMappingRepository repository, TinyUrlCache cache, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.cache = cache;
        this.meterRegistry = meterRegistry;
    }

    String getLongUrl(String shortUrl) {
        Optional<String> cachedLongUrl = cache.getLongUrl(shortUrl);
        if (cachedLongUrl.isPresent()) {
            count("get.cache.hits");
            // no data
            if (cachedLongUrl.get().equals(NULL_LONG_URL)) {
                count("get.not_found");
                throw new TinyUrlNotFoundException(shortUrl);
            }

            return cachedLongUrl.get();
        }
        count("get.cache.misses");
        Optional<UrlMapping> optionalTinyUrlEntity = repository.findByShortUrl(shortUrl);
        if (optionalTinyUrlEntity.isPresent()) {
            count("get.db.hits");
            UrlMapping urlMapping = optionalTinyUrlEntity.get();
            cache.dualPutUrlMapping(urlMapping.getShortUrl(), urlMapping.getLongUrl());
            return urlMapping.getLongUrl();
        }
        // cache no data
        count("get.not_found");
        cache.dualPutUrlMapping(shortUrl, NULL_LONG_URL);
        throw new TinyUrlNotFoundException(shortUrl);
    }

    @SneakyThrows
    String createShortUrl(String longUrl) {
        if (longUrl.startsWith("https://")) {
            longUrl = longUrl.substring(8);  // Remove the "https://" (8 characters)
        }
        longUrl = URLEncoder.encode(longUrl, StandardCharsets.UTF_8);

        // directly return existing short url if exist long url
        Optional<String> optionalCachedShortUrl = cache.getShortUrl(longUrl);
        if (optionalCachedShortUrl.isPresent() && !optionalCachedShortUrl.get().equals("empty")) {
            count("create.cache.hits");
            return optionalCachedShortUrl.get();
        }

        Optional<UrlMapping> optionalTinyUrlEntity = repository.findByLongUrl(longUrl);
        if (optionalTinyUrlEntity.isPresent()) {
            count("create.db.hits");
            UrlMapping urlMapping = optionalTinyUrlEntity.get();
            cache.dualPutUrlMapping(urlMapping.getShortUrl(), urlMapping.getLongUrl());
            return urlMapping.getShortUrl();
        }

        String shortUrl = generateShortUrl(longUrl);
        // Store in DB (URL -> Short URL)
        repository.save(new UrlMapping(shortUrl, longUrl));
        // add to cache
        cache.dualPutUrlMapping(shortUrl, longUrl);
        // TODO publish to Kafka for downstream notification

        count("create.generated");

        return shortUrl;
    }

    private String generateShortUrl(String longUrl) {
        String shortUrl = null;
        int attempt = 0;
        do {
            if (attempt > 0) {
                log.warn("Collison for Url:{}, salt:{}, last shortUrl : {}", longUrl, attempt, shortUrl);
                count("create.collisions");
            }
            shortUrl = UrlHashUtil.hashUrl(longUrl, attempt);
            // Check cache first
            if (cache.getLongUrl(shortUrl).isEmpty()) {
                // Double-check with DB (optional, depending on your false positive tolerance)
                if (!repository.existsByShortUrl(shortUrl)) {
                    return shortUrl;
                }
            }
            attempt++;
        } while (attempt < COLLISON_LIMIT);
        count("create.failures");
        // If we've exceeded the max retries, return null or handle error
        throw new RuntimeException("Unable to generate unique short URL after " + COLLISON_LIMIT + " attempts.");
    }

    private void count(String name) {
        counters.computeIfAbsent(name, n -> meterRegistry.counter(METRIC_PREFIX + "." + n)).increment();
    }


}
