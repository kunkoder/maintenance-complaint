package ahqpck.maintenance.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentComplaintCountDTO {
    private String equipmentName;
    private String equipmentCode;
    private Long totalComplaints;
}
