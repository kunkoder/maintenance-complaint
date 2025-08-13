package ahqpck.maintenance.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserWeeklySummaryDTO {
    private String userName;

    private Long totalAssignedLast7Days;

    // Open counts (by report_date)
    private Long openDay1;
    private Long openDay2;
    private Long openDay3;
    private Long openDay4;
    private Long openDay5;
    private Long openDay6;
    private Long openDay7;

    // Closed counts (by close_time)
    private Long closedDay1;
    private Long closedDay2;
    private Long closedDay3;
    private Long closedDay4;
    private Long closedDay5;
    private Long closedDay6;
    private Long closedDay7;
}
