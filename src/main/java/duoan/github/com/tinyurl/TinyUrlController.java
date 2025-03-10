package duoan.github.com.tinyurl;

import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;


@Controller
@Validated
@Observed(name = "tinyurl.observed.controller")
@Timed(value = "tinyurl.timed.controller",
        percentiles = {0.5, 0.95, 0.99},
        description = "Time taken for controller")
class TinyUrlController {
    private final TinyUrlService tinyUrlService;

    TinyUrlController(TinyUrlService tinyUrlService) {
        this.tinyUrlService = tinyUrlService;
    }


    @GetMapping({"/"})
    public String index() {
        return "index";
    }

    @PostMapping("/api/tinyurl")
    @ResponseBody
    public String createTinyUrl(@RequestBody String longUrl, HttpServletRequest request) {
        String shortUrlPath = tinyUrlService.createShortUrl(longUrl);
        // Get the domain (protocol + host) from the incoming request
        String domain = request.getRequestURL().toString().replace(request.getRequestURI(), "");
        // Create the full short URL by appending the domain to the path
        return domain + "/" + shortUrlPath;
    }

    @GetMapping("/{shortUrl}")
    public RedirectView getLongURL(
            @PathVariable
            @Pattern(regexp = "^[a-zA-Z0-9_-]{1,9}$", message = "Invalid short URL format")
            String shortUrl) {
        String longUrl = URLDecoder.decode(tinyUrlService.getLongUrl(shortUrl), StandardCharsets.UTF_8);
        if (!longUrl.startsWith("https")) {
            longUrl = "https://" + longUrl;
        }
        RedirectView redirectView = new RedirectView(longUrl);
        redirectView.setStatusCode(HttpStatus.FOUND);  // Set 302 status code
        return redirectView;
    }


}
