package ahqpck.maintenance.report.dto;

public interface DailyWorkReportEquipmentDTO {
    String getDate();
    Integer getCorrectiveMaintenanceCount();
    Integer getPreventiveMaintenanceCount();
    Integer getBreakdownCount();
    Integer getOtherCount();
}