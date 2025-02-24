package duoan.github.com.tinyurl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UrlHashUtilTest {

    @Test
    void testHashUrl() {
        String url = "https://spring.io/projects/spring-boot";
        String shortUrl1 = UrlHashUtil.hashUrl(url, 0);
        String shortUrl2 = UrlHashUtil.hashUrl(url, 1);
        assertEquals(9, shortUrl1.length());
        assertEquals(9, shortUrl2.length());
        assertNotEquals(shortUrl1, shortUrl2);
        // guarantee the url exactly same
        assertEquals(UrlHashUtil.hashUrl(url, 0), UrlHashUtil.hashUrl(url, 0));
    }

}