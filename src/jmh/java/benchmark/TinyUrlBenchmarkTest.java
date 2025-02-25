package benchmark;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.Header;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.openjdk.jmh.annotations.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
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
    private CloseableHttpClient httpClient;
    private final String baseUrl = "http://localhost:8080/"; // Your local Spring Boot API URL

    @Setup(Level.Trial)
    public void setup() {
        restTemplate = new RestTemplate();
        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom().setRedirectsEnabled(false).build())
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
    @org.openjdk.jmh.annotations.OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void benchmarkCreateTinyUrl() {
        String apiUrl = baseUrl + "api/tinyurl";
        // Sample long URL
        String longUrl = "https://www.example.com/some-very-long-url";
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, longUrl + ThreadLocalRandom.current().nextLong(), String.class);
        assert response.getStatusCode() == HttpStatus.OK;
    }

    // Benchmarking getLongURL method
    @Benchmark
    @org.openjdk.jmh.annotations.OutputTimeUnit(TimeUnit.MILLISECONDS)
    public void benchmarkRedirection() throws IOException {
        String randomShortUrl = baseUrl + RandomStringUtils.insecure().next(9, toBase64URL);
        HttpGet request = new HttpGet(randomShortUrl);
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            int statusCode = response.getStatusLine().getStatusCode();
            assert statusCode == HttpStatus.FOUND.value();
            Header locationHeader = response.getFirstHeader("Location");
            String redirectionUrl = locationHeader.getValue();
            if (!redirectionUrl.equals(baseUrl) && !redirectionUrl.contains("jsessionid")) {
                System.out.println("Redirect Location: " + redirectionUrl);
            }
        }
    }
}
