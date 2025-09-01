package ahqpck.maintenance.report.dto;

import java.time.LocalDate;

public interface DailyBreakdownDTO {
    LocalDate getDate();
    Long getBreakdownCount();
    Integer getTotalResolutionTimeMinutes();
}