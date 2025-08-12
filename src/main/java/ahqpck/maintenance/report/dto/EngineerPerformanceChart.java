package ahqpck.maintenance.report.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class EngineerPerformanceChart {
    private String name;
    private int inProgress;
    private int pending;
}