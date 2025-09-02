package ahqpck.maintenance.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyComplaintDTO {
    private String date;
    private Long open;
    private Long closed;
    private Long pending;
}