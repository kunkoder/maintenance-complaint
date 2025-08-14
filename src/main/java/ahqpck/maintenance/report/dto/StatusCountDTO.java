package ahqpck.maintenance.report.dto;

import ahqpck.maintenance.report.entity.Complaint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusCountDTO {
    private Long totalAllComplaints;
    private Long totalOpen;
    private Long totalClosed;
    private Long totalPending;

    // Ensure no null values
    public StatusCountDTO orZero() {
        this.totalAllComplaints = this.totalAllComplaints != null ? this.totalAllComplaints : 0L;
        this.totalOpen = this.totalOpen != null ? this.totalOpen : 0L;
        this.totalClosed = this.totalClosed != null ? this.totalClosed : 0L;
        this.totalPending = this.totalPending != null ? this.totalPending : 0L;
        return this;
    }
}