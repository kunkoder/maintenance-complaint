package ahqpck.maintenance.report.dto;

public interface MonthlyWorkReportDTO {
    Integer getYear();
    Integer getMonth();
    Integer getCorrectiveMaintenanceCount();
    Integer getPreventiveMaintenanceCount();
    Integer getBreakdownCount();
    Integer getOtherCount();
}