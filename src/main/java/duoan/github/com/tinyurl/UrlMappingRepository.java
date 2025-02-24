package duoan.github.com.tinyurl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.sql.Timestamp;
import java.util.Optional;

interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortUrl(String shortUrl);

    Optional<UrlMapping> findByLongUrl(String longUrl);

    Page<Long> findByCreatedAtBefore(Timestamp createdAt, Pageable pageable);

    @Query("SELECT shortUrl from UrlMapping")
    Page<String> findShortUrlList(Pageable pageable);

}
