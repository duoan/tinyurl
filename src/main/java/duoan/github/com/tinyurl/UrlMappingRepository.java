package duoan.github.com.tinyurl;

import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.Optional;

@Observed(name = "tiny_url.observed.repository")
@Timed(value = "tiny_url.timed.repository",
        percentiles = {0.5, 0.95, 0.99},
        description = "Time taken for repository")
interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    boolean existsByShortUrl(String shortUrl);

    Optional<UrlMapping> findByShortUrl(String shortUrl);

    Optional<UrlMapping> findByLongUrl(String longUrl);

    Page<Long> findByCreatedAtBefore(Timestamp createdAt, Pageable pageable);
}
