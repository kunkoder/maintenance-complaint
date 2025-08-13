package ahqpck.maintenance.report.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserDailySummaryDTO {
private String userName;
    private LocalDate date;
    private Long openCount;
    private Long closedCount;

public UserDailySummaryDTO(String userName, LocalDate date, Long openCount, Long closedCount) {
    this.userName = userName;
    this.date = date;
    this.openCount = openCount != null ? openCount : 0L;
    this.closedCount = closedCount != null ? closedCount : 0L;
}
}
