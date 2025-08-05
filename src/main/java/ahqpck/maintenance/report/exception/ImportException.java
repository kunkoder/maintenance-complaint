package ahqpck.maintenance.report.exception;

import java.util.List;

public class ImportException extends RuntimeException {
    private final List<String> rowErrors;

    public ImportException(String message, List<String> rowErrors) {
        super(message);
        this.rowErrors = rowErrors;
    }

    public List<String> getRowErrors() {
        return rowErrors;
    }
}