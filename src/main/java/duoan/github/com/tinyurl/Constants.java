package duoan.github.com.tinyurl;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {
    static final int MAX_SHORT_URL_LENGTH = 9;
    /**
     * Base64 character encodes 6 bits
     * A byte (8 bits) contributes to 1⅓ Base64 characters.
     * Thus, the <code>{@linkplain #REQUIRED_BYTE_COUNT} = {@linkplain #MAX_SHORT_URL_LENGTH} * 6 / 8</code>
     */
    static final int REQUIRED_BYTE_COUNT = (int) Math.ceil((MAX_SHORT_URL_LENGTH * 6) / 8.0);
}
