package duoan.github.com.tinyurl;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
@Log4j2
class ShortUrlBloomFilter {
    private final UrlMappingRepository urlMappingRepository;
    private final RBloomFilter<String> bloomFilter;

    public ShortUrlBloomFilter(UrlMappingRepository urlMappingRepository, RedissonClient redissonClient) {
        this.urlMappingRepository = urlMappingRepository;
        this.bloomFilter = redissonClient.getBloomFilter("ShortUrlBloomFilter");
    }

    /**
     * Check if the URL is definitely not in the filter
     * bloomFilter.contains() return:
     * - `true` if the URL might exist.
     * - `false` if the URL definitely does not exist (i.e., no false negatives).
     * False Positive: The Bloom Filter may sometimes incorrectly indicate that an element is in the filter when it isn’t.
     * False Negative: This will never happen with a Bloom Filter; if it says an element isn’t in the filter, it’s definitely not there.
     */
    public boolean definitelyNotExist(String shortenURL) {
        return !bloomFilter.contains(shortenURL);
    }

    public void addShortUrl(String shortUrl) {
        bloomFilter.add(shortUrl);
    }

    @PostConstruct
    public void init() {
        if (bloomFilter.isExists()) {
            log.info("Redis TinyUrlBloomFilter already exists");
            return;
        }
        log.info("Initializing Redis TinyUrlBloomFilter");
        bloomFilter.tryInit(62 ^ 7, 0.001);
        Pageable pageable = Pageable.ofSize(1000);
        Page<String> shortUrls;
        do {
            shortUrls = urlMappingRepository.findShortUrlList(pageable);
            if (shortUrls.isEmpty()) {
                break;
            }
            bloomFilter.add(shortUrls.getContent());
            log.info("Adding {}/{} shortUrls", shortUrls.getSize(), shortUrls.getTotalElements());
        } while (shortUrls.hasNext());
        log.info("Finished Initializing Redis TinyUrlBloomFilter");
    }

}
