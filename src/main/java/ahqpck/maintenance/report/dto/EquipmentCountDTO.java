package ahqpck.maintenance.report.dto;

public interface EquipmentCountDTO {

    String getEquipmentName();
    String getEquipmentCode();
    Integer getTotalResolutionTime();
    Long getTotalWorkReports();
    Long getTotalComplaints();
    Long getTotalOccurrences();
}