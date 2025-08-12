package ahqpck.maintenance.report.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardDTO {
    private Map<String, Long> complaintStatusCount = new HashMap<>();
    private List<WeeklyComplaintChart> weeklyData = new ArrayList<>();
    private List<EngineerPerformanceChart> engineerStats = new ArrayList<>();
    private Map<String, List<Integer>> equipmentChartData = new LinkedHashMap<>();
 
    // In DashboardDTO.java
public List<String> getWeeklyDays() {
    return weeklyData.stream()
                     .map(WeeklyComplaintChart::getDay)
                     .collect(Collectors.toList());
}

public List<Integer> getWeeklyTotals() {
    return weeklyData.stream()
                     .map(data -> data.getOpen() + data.getClosed() + data.getInProgress() + data.getPending())
                     .map(Integer::valueOf)
                     .collect(Collectors.toList());
}
}