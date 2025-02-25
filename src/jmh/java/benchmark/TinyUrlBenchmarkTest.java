package benchmark;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.openjdk.jmh.annotations.*;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
public class TinyUrlBenchmarkTest {
    private static final char[] toBase64URL = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '-', '_'
    };
    private PoolingHttpClientConnectionManager connManager;
    private CloseableHttpClient httpClient;
    private final String baseUrl = "http://localhost:8080/"; // Your local Spring Boot API URL

    @Setup(Level.Trial)
    public void setup() {
        // Connection manager for pooling across threads (if shared, but here per-thread)
        connManager = new PoolingHttpClientConnectionManager();
        connManager.setMaxTotal(50);         // Total connections across all threads
        connManager.setDefaultMaxPerRoute(20); // Max connections to localhost:8080

        RequestConfig config = RequestConfig.custom()
                .setRedirectsEnabled(false)  // No redirects
                .setConnectTimeout(2000)     // 2s connect timeout
                .setSocketTimeout(2000)      // 2s read timeout
                .setConnectionRequestTimeout(2000) // 2s to get a connection from pool
                .build();

        httpClient = HttpClients.custom()
                .setConnectionManager(connManager)
                .setDefaultRequestConfig(config)
                .setMaxConnTotal(50)         // Align with connManager
                .setMaxConnPerRoute(20)
                .setRetryHandler((exception, executionCount, context) -> executionCount < 3) // Retry up to 3 times
                .build();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }


    // Benchmarking createTinyUrl method
    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void benchmarkCreateTinyUrl() throws IOException {
        String apiUrl = baseUrl + "api/tinyurl";
        // Sample long URL
        String longUrl = "https://www.example.com/some-very-long-url" + ThreadLocalRandom.current().nextLong();

        HttpPost request = new HttpPost(apiUrl);
        request.setEntity(new StringEntity(longUrl));
        request.setHeader("Content-Type", "text/plain");
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            assert statusCode == HttpStatus.OK.value();
            EntityUtils.consume(response.getEntity());  // Ensure response is fully read
        }
    }

    // Benchmarking getLongURL method
    @Benchmark
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void benchmarkRedirection() throws IOException {
        String randomShortUrl = baseUrl + RandomStringUtils.insecure().next(9, toBase64URL);
        HttpGet request = new HttpGet(randomShortUrl);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            assert statusCode == HttpStatus.FOUND.value();
        }
    }
}
