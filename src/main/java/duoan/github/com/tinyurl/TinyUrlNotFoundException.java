package duoan.github.com.tinyurl;

class TinyUrlNotFoundException extends RuntimeException {
    TinyUrlNotFoundException(String shortUrl) {
        super("No translation exists for short '" + shortUrl + "'");
    }
}
