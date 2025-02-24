package duoan.github.com.tinyurl;

import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
class TinyUrlService {
    /// After {@linkplain #COLLISON_LIMIT} tries of re-salting long urls to avoid collisions, give up.
    private static final int COLLISON_LIMIT = 10;
    private static final String NULL_LONG_URL = "NULL";

    private final UrlMappingRepository repository;
    private final TinyUrlCache cache;
    private final ShortUrlBloomFilter bloomFilter;

    TinyUrlService(UrlMappingRepository repository, TinyUrlCache cache, ShortUrlBloomFilter bloomFilter) {
        this.repository = repository;
        this.cache = cache;
        this.bloomFilter = bloomFilter;
    }

    String getLongUrl(String shortUrl) {
        // 1. verify the shortUrl
        if (shortUrl.length() > Constants.MAX_SHORT_URL_LENGTH) {
            throw new TinyUrlNotFoundException(shortUrl);
        }

        // 2. definitely not exist
        if (bloomFilter.definitelyNotExist(shortUrl)) {
            throw new TinyUrlNotFoundException(shortUrl);
        }

        // 3. check cache
        Optional<String> cachedLongUrl = cache.getLongUrl(shortUrl);

        if (cachedLongUrl.isPresent()) {
            // no data
            if (cachedLongUrl.get().equals(NULL_LONG_URL)) {
                throw new TinyUrlNotFoundException(shortUrl);
            }

            return cachedLongUrl.get();
        }

        Optional<UrlMapping> optionalTinyUrlEntity = repository.findByShortUrl(shortUrl);
        if (optionalTinyUrlEntity.isPresent()) {
            UrlMapping urlMapping = optionalTinyUrlEntity.get();
            cache.addTinyUrl(urlMapping.getShortUrl(), urlMapping.getLongUrl());
            return urlMapping.getLongUrl();
        }
        // cache no data
        cache.addTinyUrl(shortUrl, NULL_LONG_URL);
        throw new TinyUrlNotFoundException(shortUrl);
    }

    String createShortUrl(String longUrl) {
        if (longUrl.startsWith("https://")) {
            longUrl = longUrl.substring(8);  // Remove the "https://" (8 characters)
        }
        longUrl = URLEncoder.encode(longUrl, StandardCharsets.UTF_8);

        // directly return existing short url if exist long url
        Optional<String> optionalCachedShortUrl = cache.getShortUrl(longUrl);
        if (optionalCachedShortUrl.isPresent() && !optionalCachedShortUrl.get().equals("empty")) {
            return optionalCachedShortUrl.get();
        }

        Optional<UrlMapping> optionalTinyUrlEntity = repository.findByLongUrl(longUrl);
        if (optionalTinyUrlEntity.isPresent()) {
            UrlMapping urlMapping = optionalTinyUrlEntity.get();
            cache.addTinyUrl(urlMapping.getShortUrl(), urlMapping.getLongUrl());

            return urlMapping.getShortUrl();
        }

        String shortUrl = generateShortUrl(longUrl);
        // Store in DB (URL -> Short URL)
        repository.save(new UrlMapping(shortUrl, longUrl));
        // Update bloom filter
        bloomFilter.addShortUrl(shortUrl);
        // add to cache
        cache.addTinyUrl(shortUrl, longUrl);
        // TODO publish to Kafka for downstream notification
        return shortUrl;
    }

    private String generateShortUrl(String longUrl) {
        String shortUrl;
        int attempt = 0;
        while (attempt < COLLISON_LIMIT) {
            shortUrl = UrlHashUtil.hashUrl(longUrl, attempt);
            // Check if this short URL is definitely not in the Bloom Filter
            if (bloomFilter.definitelyNotExist(shortUrl)) {
                return shortUrl;
            }
            attempt++;
        }
        // If we've exceeded the max retries, return null or handle error
        throw new RuntimeException("Unable to generate unique short URL after " + COLLISON_LIMIT + " attempts.");
    }

}
