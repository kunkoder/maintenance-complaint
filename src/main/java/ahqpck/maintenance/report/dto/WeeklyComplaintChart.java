package ahqpck.maintenance.report.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyComplaintChart {
    private String day;
    private int open;
    private int closed;
    private int inProgress;
    private int pending;
}