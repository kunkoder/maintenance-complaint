package ahqpck.maintenance.report.controller.advice;

import org.springframework.http.HttpStatus;
// import org.springframework.security.access.AccessDeniedException;
// import org.springframework.security.authentication.BadCredentialsException;
// import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import ahqpck.maintenance.report.controller.advice.CustomException.BadRequestCustomException;
import ahqpck.maintenance.report.controller.advice.CustomException.DataExistException;
import ahqpck.maintenance.report.controller.advice.CustomException.NotFoundException;

import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice(annotations = Controller.class)
public class ErrorHandler {

    // Utility method to join errors with delimiter
    private String joinErrors(List<String> errors) {
        return errors.isEmpty() ? "Invalid input" : String.join("|", errors);
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralExceptions(Exception ex, RedirectAttributes ra) {
        List<String> errors = List.of("An unexpected error occurred.");
        ra.addFlashAttribute("error", joinErrors(errors));
        return "redirect:/error"; // or redirect to a safe page like dashboard
    }

    // 400 - Validation errors (e.g., @Valid on DTOs)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidationErrors(MethodArgumentNotValidException ex, RedirectAttributes ra) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.toList());
        ra.addFlashAttribute("error", joinErrors(errors));
        return "redirect:/"; // Change to appropriate form page
    }

    // 400 - Custom Bad Request
    @ExceptionHandler(BadRequestCustomException.class)
    public String handleBadRequestCustomException(BadRequestCustomException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:/"; // Adjust target view
    }

    // 400 - File too large
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, RedirectAttributes ra) {
        String msg = "File is too large. " + ex.getMessage();
        ra.addFlashAttribute("error", msg);
        return "redirect:/"; // Adjust to upload form page
    }

    // 401 - Bad Credentials
    // @ExceptionHandler(BadCredentialsException.class)
    // public String handleBadCredentialsException(BadCredentialsException ex, RedirectAttributes ra) {
    //     ra.addFlashAttribute("error", "Invalid username or password.");
    //     return "redirect:/login"; // Always redirect to login on auth failure
    // }

    // 403 - Authorization Denied
    // @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    // public String handleAccessDeniedException(Exception ex, RedirectAttributes ra) {
    //     ra.addFlashAttribute("error", "You do not have permission to perform this action.");
    //     return "redirect:/access-denied"; // or redirect to home
    // }

    // 404 - Not Found
    @ExceptionHandler(NotFoundException.class)
    public String handleNotFoundException(NotFoundException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:/"; // Or back to list page
    }

    // 409 - Conflict / Data Exists
    @ExceptionHandler(DataExistException.class)
    public String handleDataExistException(DataExistException ex, RedirectAttributes ra) {
        ra.addFlashAttribute("error", ex.getMessage());
        return "redirect:/"; // Back to form or list
    }
}