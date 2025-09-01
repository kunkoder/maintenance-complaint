package ahqpck.maintenance.report.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Date;
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

    /**
     * Converts an Object to duration in minutes.
     * Handles:
     * - Integer/Double values (assumed to be minutes)
     * - Excel serial time (fraction of a day)
     * - String "HH:mm", "H:mm", "HH:mm:ss"
     */
    public Integer toDurationInMinutes(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) {
            return null;
        }

        // Case 1: It's a number (Double, Integer, etc.)
        if (obj instanceof Number) {
            double value = ((Number) obj).doubleValue();

            // If value < 1 → likely fraction of a day (Excel time)
            if (value < 1.0) {
                return (int) Math.round(value * 24 * 60); // days → minutes
            } else {
                // Assume it's already in minutes
                return ((Number) obj).intValue();
            }
        }

        String str = obj.toString().trim();

        // Case 2: Excel serial date as string
        if (str.matches("^\\d+(\\.\\d+)?$")) {
            try {
                double serial = Double.parseDouble(str);
                if (serial < 1.0) {
                    return (int) Math.round(serial * 24 * 60); // fraction of day → minutes
                } else {
                    return (int) serial; // assume it's minutes
                }
            } catch (NumberFormatException ignored) {
            }
        }

        // Case 3: Time string like "1:30", "2:45:00"
        if (str.matches("^\\d{1,2}:\\d{2}(:\\d{2})?$")) {
            try {
                String[] parts = str.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

                return (int) Math.round(hours * 60 + minutes + seconds / 60.0);
            } catch (Exception ignored) {
                throw new IllegalArgumentException("Invalid time format (HH:mm or HH:mm:ss): " + str);
            }
        }

        throw new IllegalArgumentException("Cannot parse duration: " + str);
    }

    public LocalDate toLocalDate(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) {
            return null;
        }

        if (obj instanceof Date) {
            System.out.println("report date util shit");
            return ((Date) obj).toInstant()
                    // .atZone(ZoneId.of("Asia/Riyadh"))
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDate()
                    .plusDays(1);
        }

        String str = obj.toString().trim();

        System.out.println("is date = " + (obj instanceof Date));
        

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
                "MMM yyyy", // "Apr 2014"
                "MMMM yyyy", // "April 2014"
                "MM/yyyy", // "04/2014"
                "M/yyyy", // "4/2014"
                "yyyy-MM", // "2014-04"
                "yyyy/MM")
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
        if (n >= 60)
            n--; // Excel 1900 leap year bug
        return LocalDate.of(1899, 12, 31).plusDays(n);
    }

    public LocalDateTime toLocalDateTime(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty()) {
            return null;
        }

        if (obj instanceof Date) {
            return ((Date) obj).toInstant()
            // .atZone(ZoneId.of("Asia/Riyadh"))
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDateTime()
                    .plusHours(3);
        }

        String str = obj.toString().trim();

        // Handle Excel serial date/time (e.g., 45356.625 = date + fraction of day)
        if (str.matches("\\d+(\\.\\d+)?")) {
            double serial = Double.parseDouble(str);
            return convertExcelDateTime(serial);
        }

        // Define common LocalDateTime formats
        return Stream.of(
                // ISO formats
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm",
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",

                // Custom formats with seconds
                "dd/MM/yyyy HH:mm:ss",
                "MM/dd/yyyy HH:mm:ss",
                "dd-MM-yyyy HH:mm:ss",
                "MM-dd-yyyy HH:mm:ss",

                // Without seconds
                "dd/MM/yyyy HH:mm",
                "MM/dd/yyyy HH:mm",
                "dd-MM-yyyy HH:mm",
                "MM-dd-yyyy HH:mm",

                // With AM/PM (12-hour)
                "dd/MM/yyyy h:mm a",
                "MM/dd/yyyy h:mm a",
                "dd-MM-yyyy h:mm a",
                "MM-dd-yyyy h:mm a",
                "yyyy-MM-dd h:mm a")
                .map(pattern -> {
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                        return LocalDateTime.parse(str, formatter);
                    } catch (DateTimeParseException ignored) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid date-time format: " + str));
    }

    private static LocalDateTime convertExcelDateTime(double serial) {
        // Excel's epoch starts from Dec 30, 1899
        // Handle 1900 leap year bug
        int days = (int) serial;
        double fractionalDay = serial - days;

        if (days >= 60)
            days--; // Excel bug: treats 1900 as leap year

        LocalDateTime base = LocalDateTime.of(1899, 12, 30, 0, 0);
        return base.plusDays(days).plusNanos((long) (fractionalDay * 24 * 60 * 60 * 1_000_000_000L));
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