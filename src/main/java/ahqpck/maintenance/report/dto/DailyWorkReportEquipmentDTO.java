package ahqpck.maintenance.report.dto;

public interface DailyWorkReportEquipmentDTO {
    String getDate();
    String getEquipmentCode();
    Integer getCorrectiveMaintenanceCount();
    Integer getPreventiveMaintenanceCount();
    Integer getBreakdownCount();
    Integer getOtherCount();
}