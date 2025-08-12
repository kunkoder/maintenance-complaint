package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.DashboardDTO;
import ahqpck.maintenance.report.dto.EngineerPerformanceChart;
import ahqpck.maintenance.report.dto.WeeklyComplaintChart;
import ahqpck.maintenance.report.entity.*;
import ahqpck.maintenance.report.repository.ComplaintRepository;
import ahqpck.maintenance.report.repository.DashboardRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// src/main/java/com/ahqpck/maintenance/report/service/DashboardService.java
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final ComplaintRepository complaintRepository;

    public DashboardDTO getDashboardData() {
        DashboardDTO dto = new DashboardDTO();

        // 1. Overall Status
        List<Object[]> statusData = dashboardRepository.countComplaintsByStatus();
        Map<String, Long> statusMap = statusData.stream()
                .collect(Collectors.toMap(
                        o -> o[0].toString(),
                        o -> (Long) o[1]
                ));
        dto.setComplaintStatusCount(statusMap);

        // 2. Weekly Data (Last 7 Days)
        List<Object[]> weeklyRaw = dashboardRepository.countComplaintsLast7Days();
        Map<Integer, WeeklyComplaintChart> dayMap = new HashMap<>();
        for (int i = 1; i <= 7; i++) {
            WeeklyComplaintChart w = new WeeklyComplaintChart();
            w.setDay(String.valueOf("SMTWTFS".charAt(i % 7))); // S,M,T,W,T,F,S
            w.setOpen(0); w.setClosed(0); w.setInProgress(0); w.setPending(0);
            dayMap.put(i, w);
        }

        for (Object[] row : weeklyRaw) {
            Integer dayOfWeek = ((Number) row[0]).intValue(); // 1=Sun, 2=Mon...
            String status = row[1].toString();
            Long count = (Long) row[2];

            WeeklyComplaintChart w = dayMap.get(dayOfWeek);
            if (Complaint.Status.valueOf(status) == Complaint.Status.OPEN) {
                w.setOpen(count.intValue());
            } else if (status.equals("CLOSED")) {
                w.setClosed(count.intValue());
            } else if (status.equals("IN_PROGRESS")) {
                w.setInProgress(count.intValue());
            } else if (status.equals("PENDING")) {
                w.setPending(count.intValue());
            }
        }

        dto.setWeeklyData(new ArrayList<>(dayMap.values()));

        // 3. Engineer Stats
        List<Object[]> inProgress = dashboardRepository.countComplaintsByAssigneeAndStatus(Complaint.Status.IN_PROGRESS);
        List<Object[]> pending = dashboardRepository.countComplaintsByAssigneeAndStatus(Complaint.Status.PENDING);

        Map<String, EngineerPerformanceChart> engineerMap = new HashMap<>();

        // For IN_PROGRESS
List<Object[]> inProgressList = dashboardRepository.countComplaintsByAssigneeAndStatus(Complaint.Status.IN_PROGRESS);
for (Object[] row : inProgressList) {
    String name = (String) row[0];
    Long count = (Long) row[1];  // ← index 1, not 2

    engineerMap.computeIfAbsent(name, k -> new EngineerPerformanceChart())
               .setInProgress(count.intValue())
               .setName(name);
}

// For PENDING
List<Object[]> pendingList = dashboardRepository.countComplaintsByAssigneeAndStatus(Complaint.Status.PENDING);
for (Object[] row : pendingList) {
    String name = (String) row[0];
    Long count = (Long) row[1];  // ← index 1

    engineerMap.computeIfAbsent(name, k -> new EngineerPerformanceChart())
               .setPending(count.intValue())
               .setName(name);
}

        dto.setEngineerStats(new ArrayList<>(engineerMap.values()));

        // 5. Equipment Chart (example: total complaints per equipment)
        // You can expand this later
        dto.getEquipmentChartData().put("Open", Arrays.asList(256, 230, 245, 287, 240, 250, 230, 295, 331, 431, 456, 521));
        dto.getEquipmentChartData().put("In Progress", Arrays.asList(542, 480, 430, 550, 530, 453, 380, 434, 568, 610, 700, 900));
        dto.getEquipmentChartData().put("Pending", Arrays.asList(1200, 1300, 1250, 1400, 1350, 1450, 1500, 1600, 1700, 1800, 1900, 2000));
        dto.getEquipmentChartData().put("Closed", Arrays.asList(154, 184, 175, 203, 210, 231, 240, 278, 252, 312, 320, 374));

        return dto;
    }
}