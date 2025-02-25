package duoan.github.com.tinyurl;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

@ControllerAdvice
class TinyUrlControllerAdvice {

    private final MeterRegistry meterRegistry;

    TinyUrlControllerAdvice(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    RedirectView handleValidationException(ConstraintViolationException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "Validation failed: " + ex.getMessage());
        meterRegistry.gauge("tinyurl.gauge.controller.validationErrors", ex.getConstraintViolations().size());
        return new RedirectView("/");
    }

    @ExceptionHandler(TinyUrlNotFoundException.class)
    RedirectView handleTinyUrlNotFoundException(TinyUrlNotFoundException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", ex.getMessage());
        meterRegistry.gauge("tinyurl.gauge.controller.not_found", 1);
        return new RedirectView("/");
    }
}
