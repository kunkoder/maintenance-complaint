package ahqpck.maintenance.report.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssigneeDailyStatusDetailDTO {
    private String assignee;
    private List<Integer> open;
    private List<Integer> pending;
    private List<Integer> closed;
}