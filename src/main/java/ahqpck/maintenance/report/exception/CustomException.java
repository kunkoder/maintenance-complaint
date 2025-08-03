package ahqpck.maintenance.report.exception;

/**
 * Unified Custom Exception Class for the Maintenance Report System.
 * Contains all application-specific exceptions as static subclasses.
 */
public class CustomException {

    /**
     * Thrown when a requested entity (e.g., Part, User, Complaint) is not found.
     */
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when a validation rule is violated (e.g., missing required field).
     */
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when an uploaded image exceeds the allowed size (e.g., >1MB).
     */
    public static class ImageTooLargeException extends RuntimeException {
        public ImageTooLargeException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when an uploaded file is not a valid image.
     */
    public static class InvalidImageException extends RuntimeException {
        public InvalidImageException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when a file operation (save/delete) fails.
     */
    public static class FileStorageException extends RuntimeException {
        public FileStorageException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when a unique constraint is violated (e.g., duplicate code).
     */
    public static class DuplicateResourceException extends RuntimeException {
        public DuplicateResourceException(String message) {
            super(message);
        }
    }

    /**
     * Thrown when inventory operation fails (e.g., insufficient stock).
     */
    public static class InsufficientStockException extends RuntimeException {
        public InsufficientStockException(String message) {
            super(message);
        }
    }

    // Add more custom exceptions here as needed
    // Example:
    // public static class UnauthorizedAccessException extends RuntimeException { ... }
}