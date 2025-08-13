package ahqpck.maintenance.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserComplaintSummaryDTO {
    private String userName;
    private Long totalComplaintsAssigned;
    private Long totalOpen;
    private Long totalClosed;
    private Long totalPending;
    private Long totalInProgress;
}
