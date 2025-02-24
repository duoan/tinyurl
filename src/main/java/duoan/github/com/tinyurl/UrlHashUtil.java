package duoan.github.com.tinyurl;

import com.google.common.hash.Hashing;
import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Log4j2
@UtilityClass
class UrlHashUtil {

    String hashUrl(String url, int salt) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        byte[] hashBytes = Hashing.goodFastHash(160).hashString(url + salt, StandardCharsets.UTF_8).asBytes();
        byte[] shiftedBytes = new byte[Constants.REQUIRED_BYTE_COUNT];
        System.arraycopy(hashBytes, 0, shiftedBytes, 0, Constants.REQUIRED_BYTE_COUNT);
        // Encode to Base64
        String base64Hash = Base64.getUrlEncoder().withoutPadding().encodeToString(shiftedBytes);
        // Ensure it's exactly 9 characters

        return base64Hash.substring(0, Constants.MAX_SHORT_URL_LENGTH);
    }
}
