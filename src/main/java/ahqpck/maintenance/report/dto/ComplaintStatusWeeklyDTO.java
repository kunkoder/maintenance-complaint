package ahqpck.maintenance.report.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ComplaintStatusWeeklyDTO {
    private String date;
    private String dayLabel; // e.g., "Day-1", "Day-2"

    private Long open;
    private Long inProgress;
    private Long pending;
    private Long done;
    private Long closed;
}
