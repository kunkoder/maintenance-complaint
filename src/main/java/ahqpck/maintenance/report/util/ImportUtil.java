package ahqpck.maintenance.report.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

@Component
public class ImportUtil {

    public String toString(Object obj) {
        return obj != null ? obj.toString().trim() : null;
    }

    public Integer toInteger(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + obj, e);
        }
    }

    public LocalDate toLocalDate(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) {
            return null;
        }

        String str = obj.toString().trim();

        // Handle Excel serial date
        if (str.matches("\\d+(\\.\\d+)?")) {
            double serial = Double.parseDouble(str);
            return convertExcelDate(serial);
        }

        // Try multiple date formats
        return Stream.of(
                // Full date formats
                "yyyy-MM-dd",
                "dd/MM/yyyy", "MM/dd/yyyy",
                "dd-MM-yyyy", "MM-dd-yyyy",

                // Month-year formats
                "MMM yyyy",     // "Apr 2014"
                "MMMM yyyy",    // "April 2014"
                "MM/yyyy",      // "04/2014"
                "M/yyyy",       // "4/2014"
                "yyyy-MM",      // "2014-04"
                "yyyy/MM"
        )
        .map(pattern -> {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return formatter.parse(str);
            } catch (Exception ignored) {
                return null;
            }
        })
        .filter(Objects::nonNull)
        .map(parsed -> {
            // If only year and month are present, default day to 1
            if (parsed.isSupported(ChronoField.YEAR) && parsed.isSupported(ChronoField.MONTH_OF_YEAR)) {
                int year = parsed.get(ChronoField.YEAR);
                int month = parsed.get(ChronoField.MONTH_OF_YEAR);
                return LocalDate.of(year, month, 1);
            }
            // If day is present, parse as full date
            if (parsed.isSupported(ChronoField.DAY_OF_MONTH)) {
                return LocalDate.from(parsed);
            }
            return null;
        })
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid date format: " + str));
    }

    private static LocalDate convertExcelDate(double serial) {
        int n = (int) serial;
        if (n >= 60) n--; // Excel 1900 leap year bug
        return LocalDate.of(1899, 12, 30).plusDays(n);
    }

    public static class ImportResult {
        private final int importedCount;
        private final List<String> errorMessages;

        public ImportResult(int importedCount, List<String> errorMessages) {
            this.importedCount = importedCount;
            this.errorMessages = List.copyOf(errorMessages); // Immutable copy
        }

        public int getImportedCount() {
            return importedCount;
        }

        public List<String> getErrorMessages() {
            return errorMessages;
        }

        public boolean hasErrors() {
            return !errorMessages.isEmpty();
        }
    }
}