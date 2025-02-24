package duoan.github.com.tinyurl;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

/**
 * Reference: <a href="https://jakarta.ee/specifications/persistence/">Jakarta Persistence</a>
 */
@Entity
@Table(name = "t_url_mappings")
@Data
@NoArgsConstructor
public class UrlMapping {
    /**
     * We are using base64 to encode a long url,
     * hence 64**9 will be able store 18,014,398,509,481,984 [18 Quadrillion] entities
     */
    @Id
    @Column(name = "short_url", nullable = false, unique = true, length = 9)
    private String shortUrl;
    @Column(name = "long_url", nullable = false, unique = true, length = 2048)
    private String longUrl;
    /**
     * The urls will be expired when creationTime + 10 years
     */
    @Column(name = "created_at")
    @ColumnDefault("now()")
    private Instant createdAt;

    public UrlMapping(String shortUrl, String longUrl) {
        this.shortUrl = shortUrl;
        this.longUrl = longUrl;
        this.createdAt = Instant.now();
    }
}
