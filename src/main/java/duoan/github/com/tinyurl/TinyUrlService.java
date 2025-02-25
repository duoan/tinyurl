package duoan.github.com.tinyurl;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Log4j2
@Service
class TinyUrlService {
    /// After {@linkplain #COLLISON_LIMIT} tries of re-salting long urls to avoid collisions, give up.
    private static final int COLLISON_LIMIT = 100;
    private static final String NULL_LONG_URL = "NULL";

    private final UrlMappingRepository repository;
    private final TinyUrlCache cache;
    private final MeterRegistry meterRegistry;

    TinyUrlService(UrlMappingRepository repository, TinyUrlCache cache, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.cache = cache;
        this.meterRegistry = meterRegistry;
    }

    String getLongUrl(String shortUrl) {
        // 1. verify the shortUrl
        if (shortUrl.length() > Constants.MAX_SHORT_URL_LENGTH) {
            meterRegistry.gauge("tiny_url.gauge.service.get.invalid_short_url", 1);
            throw new TinyUrlNotFoundException(shortUrl);
        }

        // 2. check cache
        Optional<String> cachedLongUrl = cache.getLongUrl(shortUrl);

        if (cachedLongUrl.isPresent()) {
            meterRegistry.gauge("tiny_url.gauge.service.get.long_url_cache_hit", 1);
            // no data
            if (cachedLongUrl.get().equals(NULL_LONG_URL)) {
                meterRegistry.gauge("tiny_url.gauge.service.get.long_url_cache_null", 1);
                throw new TinyUrlNotFoundException(shortUrl);
            }

            return cachedLongUrl.get();
        }

        Optional<UrlMapping> optionalTinyUrlEntity = repository.findByShortUrl(shortUrl);
        if (optionalTinyUrlEntity.isPresent()) {
            meterRegistry.gauge("tiny_url.gauge.service.get.long_url_db_hit", 1);
            UrlMapping urlMapping = optionalTinyUrlEntity.get();
            cache.dualPutUrlMapping(urlMapping.getShortUrl(), urlMapping.getLongUrl());
            return urlMapping.getLongUrl();
        }
        // cache no data
        meterRegistry.gauge("tiny_url.gauge.service.get.long_url_not_exist", 1);
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
            meterRegistry.gauge("tiny_url.gauge.service.create.short_url_cache_hit", 1);
            return optionalCachedShortUrl.get();
        }

        Optional<UrlMapping> optionalTinyUrlEntity = repository.findByLongUrl(longUrl);
        if (optionalTinyUrlEntity.isPresent()) {
            meterRegistry.gauge("tiny_url.gauge.service.create.short_url_db_hit", 1);
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

        meterRegistry.gauge("tiny_url.gauge.service.create.short_url_generated", 1);

        return shortUrl;
    }

    private String generateShortUrl(String longUrl) {
        String shortUrl = null;
        int attempt = 0;
        do {
            if (attempt > 0) {
                log.warn("Collison for Url:{}, salt:{}, last shortUrl : {}", longUrl, attempt, shortUrl);
            }
            shortUrl = UrlHashUtil.hashUrl(longUrl, attempt);
            // Check cache first
            if (cache.getLongUrl(shortUrl).isEmpty()) {
                // Double-check with DB (optional, depending on your false positive tolerance)
                if (!repository.existsByShortUrl(shortUrl)) {
                    return shortUrl;
                }
            }
            meterRegistry.gauge("tiny_url.gauge.service.create.short_url_collison", 0);

            attempt++;
        } while (attempt < COLLISON_LIMIT);
        meterRegistry.gauge("tiny_url.gauge.service.create.short_url_failed", 1);
        // If we've exceeded the max retries, return null or handle error
        throw new RuntimeException("Unable to generate unique short URL after " + COLLISON_LIMIT + " attempts.");
    }


}
