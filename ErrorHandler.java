package ahqpck.maintenance.report.controller.advice;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import ahqpck.maintenance.report.controller.advice.CustomException.BadRequestCustomException;
import ahqpck.maintenance.report.controller.advice.CustomException.DataExistException;
import ahqpck.maintenance.report.controller.advice.CustomException.NotFoundException;
import com.example.demo.dto.Response;

@ControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(Exception.class)
    public final ResponseEntity<Response<Object>> handleGeneralExceptions(Exception ex) {
        List<String> errorList = Collections.singletonList(ex.getMessage());
        return new ResponseEntity<>(mappingError(HttpStatus.INTERNAL_SERVER_ERROR.value(),
            HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(), errorList),
            new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<String> errorList = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage).collect(Collectors.toList());
        return new ResponseEntity<>(mappingError(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST
            .getReasonPhrase(), errorList), new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    // 400
    @ExceptionHandler(BadRequestCustomException.class)
    public final ResponseEntity<Response<Object>> handleBadRequestCustomException(BadRequestCustomException ex) {
        List<String> errors = Collections.singletonList(ex.getMessage());
        return new ResponseEntity<>(mappingError(HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(), errors),
            new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    // 400
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public final ResponseEntity<Response<Object>> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        List<String> errors = Collections.singletonList(ex.getMessage());
        return new ResponseEntity<>(mappingError(HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.getReasonPhrase(), errors),
            new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    // 401
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Response<Object>> handleBadCredentialsException(BadCredentialsException ex) {
        List<String> errors = Collections.singletonList(ex.getMessage());
        return new ResponseEntity<>(mappingError(HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(), errors),
            new HttpHeaders(), HttpStatus.UNAUTHORIZED);
    }

    // 403
    @ExceptionHandler(AuthorizationDeniedException.class)
    public final ResponseEntity<Response<Object>> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
        List<String> errors = Collections.singletonList(ex.getMessage());
        return new ResponseEntity<>(mappingError(HttpStatus.FORBIDDEN.value(),
            HttpStatus.FORBIDDEN.getReasonPhrase(), errors),
            new HttpHeaders(), HttpStatus.FORBIDDEN);
    }

    // 403
    @ExceptionHandler(AccessDeniedException.class)
    public final ResponseEntity<Response<Object>> handleAccessDeniedException(AccessDeniedException ex) {
        List<String> errors = Collections.singletonList(ex.getMessage());
        return new ResponseEntity<>(mappingError(HttpStatus.FORBIDDEN.value(),
            HttpStatus.FORBIDDEN.getReasonPhrase(), errors),
            new HttpHeaders(), HttpStatus.FORBIDDEN);
    }

    // 404
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Response<Object>> handleNotFoundException(NotFoundException ex) {
        List<String> errors = Collections.singletonList(ex.getMessage());
        return new ResponseEntity<>(mappingError(HttpStatus.NOT_FOUND.value(),
            HttpStatus.NOT_FOUND.getReasonPhrase(), errors),
            new HttpHeaders(), HttpStatus.NOT_FOUND);
    }

    // 409
    @ExceptionHandler(DataExistException.class)
    public final ResponseEntity<Response<Object>> handleRuntimeExceptions(DataExistException ex) {
        List<String> errors = Collections.singletonList(ex.getMessage());
        return new ResponseEntity<>(mappingError(HttpStatus.CONFLICT.value(),
            HttpStatus.CONFLICT.getReasonPhrase(), errors),
            new HttpHeaders(), HttpStatus.CONFLICT);
    }

    private Response<Object> mappingError(int responseCode, String responseMessage, List<String> errorList) {
        return Response.builder()
            .responseCode(responseCode)
            .responseMessage(responseMessage)
            .errorList(errorList)
            .build();
    }
}
