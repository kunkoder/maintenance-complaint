package ahqpck.maintenance.report.dto;

public interface MonthlyBreakdownDTO {
    int getYear();
    int getMonth();
    Long getBreakdownCount();
    Integer getTotalResolutionTimeMinutes();
}