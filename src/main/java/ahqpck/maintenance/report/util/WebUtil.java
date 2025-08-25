package ahqpck.maintenance.report.util;

import org.springframework.validation.BindingResult;

import java.util.UUID;
import java.util.stream.Collectors;

public class WebUtil {

    /**
     * Extracts validation errors from BindingResult into a single string.
     * Format: "Field1: Error1 | Field2: Error2"
     */
    public static String getErrorMessage(BindingResult bindingResult) {
        return bindingResult.getAllErrors().stream()
                .map(error -> {
                    String field = (error instanceof org.springframework.validation.FieldError)
                            ? ((org.springframework.validation.FieldError) error).getField()
                            : "Input";
                    String message = error.getDefaultMessage();
                    return field + ": " + message;
                })
                .collect(Collectors.joining(" | "));
    }

    /**
     * Returns true if errors exist and are non-empty
     */
    public static boolean hasErrors(BindingResult bindingResult) {
        return bindingResult.hasErrors();
    }

    public static String generateActivationToken() {
        return UUID.randomUUID().toString(); // 36-char unique token
    }
}