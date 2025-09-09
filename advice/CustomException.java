package ahqpck.maintenance.report.controller.advice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public interface CustomException {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public class DataExistException extends RuntimeException {
        public DataExistException(String message) {
            super(message);
        }
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public class BadRequestCustomException extends RuntimeException {
        public BadRequestCustomException(String message) {
            super(message);
        }
    }
}
