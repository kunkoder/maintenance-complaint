package ahqpck.maintenance.report.dto;

public interface DailyWorkReportDTO {
    String getDate();
    Integer getCorrectiveMaintenanceCount();
    Integer getPreventiveMaintenanceCount();
    Integer getBreakdownCount();
    Integer getOtherCount();
}