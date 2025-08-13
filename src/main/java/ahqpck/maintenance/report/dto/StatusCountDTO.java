package ahqpck.maintenance.report.dto;

import ahqpck.maintenance.report.entity.Complaint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusCountDTO {
    private Complaint.Status status;
    private Long count;
}
