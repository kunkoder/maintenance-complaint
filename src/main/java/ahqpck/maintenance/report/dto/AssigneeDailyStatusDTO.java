package ahqpck.maintenance.report.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssigneeDailyStatusDTO {
    private List<String> dates;
    private List<AssigneeDailyStatusDetailDTO> data;
}
