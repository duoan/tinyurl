package benchmark;

import org.apache.commons.lang3.RandomStringUtils;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(org.openjdk.jmh.annotations.Scope.Thread)
public class TinyUrlBenchmarkTest {
    private static final char[] toBase64URL = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'
    };
    private RestTemplate restTemplate;

    private final String baseUrl = "http://localhost:8080"; // Your local Spring Boot API URL
    private String shortUrl;

    @Setup
    public void setup() {
        restTemplate = new RestTemplate();
    }

    // Benchmarking createTinyUrl method
    @Benchmark
    @org.openjdk.jmh.annotations.OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void benchmarkCreateTinyUrl() {
        String apiUrl = baseUrl + "/api/tinyurl";
        // Sample long URL
        String longUrl = "https://www.example.com/some-very-long-url";
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, longUrl + ThreadLocalRandom.current().nextLong(), String.class);
        shortUrl = response.getBody();
    }

    // Benchmarking getLongURL method
    @Benchmark
    @org.openjdk.jmh.annotations.OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void benchmarkGetLongURL() {
        String randomShortUrl = shortUrl == null ?
                baseUrl + "/" + RandomStringUtils.insecure().next(9, toBase64URL) : shortUrl;
        ResponseEntity<String> response = restTemplate.getForEntity(randomShortUrl, String.class);
        assert response.getStatusCode() == HttpStatus.FOUND || response.getStatusCode() == HttpStatus.NOT_FOUND;

    }
}
