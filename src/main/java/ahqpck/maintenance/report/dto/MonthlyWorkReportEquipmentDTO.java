package ahqpck.maintenance.report.dto;

public interface MonthlyWorkReportEquipmentDTO {
    Integer getYear();
    Integer getMonth();
    Integer getCorrectiveMaintenanceCount();
    Integer getPreventiveMaintenanceCount();
    Integer getBreakdownCount();
    Integer getOtherCount();
}