package ahqpck.maintenance.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyComplaintDTO {
    private String date;   // Format: "YYYY-MM"
    private Long open;
    private Long closed;
    private Long pending;
}