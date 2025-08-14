package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.AssigneeDailyStatusDTO;
import ahqpck.maintenance.report.dto.AssigneeDailyStatusDetailDTO;
import ahqpck.maintenance.report.dto.ComplaintStatusWeeklyDTO;
import ahqpck.maintenance.report.dto.DailyStatusCountDTO;
import ahqpck.maintenance.report.dto.DashboardDTO;
import ahqpck.maintenance.report.dto.EngineerPerformanceChart;
import ahqpck.maintenance.report.dto.EquipmentComplaintCountDTO;
import ahqpck.maintenance.report.dto.StatusCountDTO;
import ahqpck.maintenance.report.dto.UserComplaintSummaryDTO;
import ahqpck.maintenance.report.dto.UserDailySummaryDTO;
import ahqpck.maintenance.report.dto.UserWeeklySummaryDTO;
import ahqpck.maintenance.report.dto.WeeklyComplaintChart;
import ahqpck.maintenance.report.entity.*;
import ahqpck.maintenance.report.repository.ComplaintRepository;
import ahqpck.maintenance.report.repository.DashboardRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// src/main/java/com/ahqpck/maintenance/report/service/DashboardService.java
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    // public List<StatusCountDTO> getStatusCounts() {
    // return getStatusCounts(null, null);
    // }

    // public StatusCountDTO getStatusCount(LocalDateTime from, LocalDateTime to) {
    // return dashboardRepository.getStatusCount(from, to);
    // }

    public StatusCountDTO getStatusCount(LocalDateTime from, LocalDateTime to) {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime defaultFrom = now.toLocalDate().atStartOfDay();
        LocalDateTime defaultTo = now.toLocalDate().plusDays(1).atStartOfDay(); // tomorrow

        LocalDateTime effectiveFrom = from != null ? from : defaultFrom;
        LocalDateTime effectiveTo = to != null ? to : defaultTo;

        return dashboardRepository.getStatusCount(effectiveFrom, effectiveTo);
    }

    public List<DailyStatusCountDTO> getDailyStatusCount(LocalDateTime from, LocalDateTime to) {
        LocalDateTime defaultTo = LocalDateTime.now().with(LocalTime.MAX);
        LocalDateTime defaultFrom = defaultTo.minusDays(6);

        LocalDateTime effectiveFrom = from != null ? from : defaultFrom;
        LocalDateTime effectiveTo = to != null ? to : defaultTo;

        return dashboardRepository.getDailyStatusCount(effectiveFrom, effectiveTo);
    }

    public AssigneeDailyStatusDTO getAssigneeDailyStatus(LocalDateTime from, LocalDateTime to) {
        // Default: last 7 days
        if (from == null || to == null) {
            to = LocalDateTime.now().with(LocalTime.MAX);
            from = to.minusDays(2); // last 7 days
        }

        LocalDate fromDate = from.toLocalDate();
        LocalDate toDate = to.toLocalDate();

        // Validate: from <= to
        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Invalid date range: 'from' must be before or equal to 'to'");
        }

        List<Object[]> results = dashboardRepository.getAssigneeDailyStatus(fromDate, toDate);

        // Generate all dates in range
        List<LocalDate> dateList = Stream.iterate(fromDate, d -> d.plusDays(1))
                .takeWhile(d -> !d.isAfter(toDate))
                .collect(Collectors.toList());

        int numDays = dateList.size();

        // Initialize map for assignees
        Map<String, AssigneeDailyStatusDetailDTO> assigneeMap = new LinkedHashMap<>();

        // Initialize all assignees with zero-filled arrays
        for (LocalDate date : dateList) {
            for (Object[] row : results) {
                String assignee = (String) row[0];
                assigneeMap.computeIfAbsent(assignee, k -> {
                    AssigneeDailyStatusDetailDTO dto = new AssigneeDailyStatusDetailDTO();
                    dto.setAssignee(k);
                    List<Integer> zeros = Collections.nCopies(numDays, 0);
                    dto.setOpen(new ArrayList<>(zeros));
                    dto.setPending(new ArrayList<>(zeros));
                    dto.setClosed(new ArrayList<>(zeros));
                    return dto;
                });
            }
        }

        // Fill in the data
        for (Object[] row : results) {
            String assignee = (String) row[0];
            String status = (String) row[1];
            LocalDate reportDate = ((java.sql.Date) row[2]).toLocalDate();
            Long count = ((Number) row[3]).longValue();

            // Skip if date not in range
            if (!dateList.contains(reportDate))
                continue;

            int dayIndex = dateList.indexOf(reportDate);
            AssigneeDailyStatusDetailDTO dto = assigneeMap.get(assignee);

            if ("OPEN".equals(status)) {
                List<Integer> open = dto.getOpen();
                open.set(dayIndex, Math.toIntExact(count));
                dto.setOpen(open);
            } else if ("PENDING".equals(status)) {
                List<Integer> pending = dto.getPending();
                pending.set(dayIndex, Math.toIntExact(count));
                dto.setPending(pending);
            } else if ("CLOSED".equals(status)) {
                List<Integer> closed = dto.getClosed();
                closed.set(dayIndex, Math.toIntExact(count));
                dto.setClosed(closed);
            }
        }

        // Build response
        AssigneeDailyStatusDTO response = new AssigneeDailyStatusDTO();
        response.setDates(dateList.stream().map(LocalDate::toString).collect(Collectors.toList()));
        response.setData(new ArrayList<>(assigneeMap.values()));

        return response;
    }

    public List<UserComplaintSummaryDTO> getUserComplaintSummary() {
        return dashboardRepository.getUserComplaintSummary();
    }

    // DashboardService.java
    // DashboardService.java
    public List<UserWeeklySummaryDTO> getUserWeeklySummary(LocalDateTime from, LocalDateTime to) {
        // Set default: last 7 days ending at yesterday
        if (from == null || to == null) {
            LocalDateTime now = LocalDateTime.now();
            to = now; // today
            from = now.minusDays(7); // 7 days ago
        }
        return dashboardRepository.getUserWeeklySummary(from, to);
    }

    public List<UserDailySummaryDTO> getUserDailySummary(LocalDateTime from, LocalDateTime to) {
        return dashboardRepository.getUserDailySummary(from, to);
    }

    // DashboardService.java
    // DashboardService.java
    public List<ComplaintStatusWeeklyDTO> getComplaintStatusWeekly(LocalDateTime from, LocalDateTime to) {
        return dashboardRepository.getComplaintStatusWeekly(from, to);
    }

    public List<EquipmentComplaintCountDTO> getEquipmentComplaintCount() {
        return dashboardRepository.getEquipmentComplaintCount();
    }

    // public DashboardDTO getDashboardData() {
    // DashboardDTO dto = new DashboardDTO();

    // // 1. Overall Status
    // List<Object[]> statusData = dashboardRepository.countComplaintsByStatus();
    // Map<String, Long> statusMap = statusData.stream()
    // .collect(Collectors.toMap(
    // o -> o[0].toString(),
    // o -> (Long) o[1]
    // ));
    // dto.setComplaintStatusCount(statusMap);

    // // 2. Weekly Data (Last 7 Days)
    // List<Object[]> weeklyRaw = dashboardRepository.countComplaintsLast7Days();
    // Map<Integer, WeeklyComplaintChart> dayMap = new HashMap<>();
    // for (int i = 1; i <= 7; i++) {
    // WeeklyComplaintChart w = new WeeklyComplaintChart();
    // w.setDay(String.valueOf("SMTWTFS".charAt(i % 7))); // S,M,T,W,T,F,S
    // w.setOpen(0); w.setClosed(0); w.setInProgress(0); w.setPending(0);
    // dayMap.put(i, w);
    // }

    // for (Object[] row : weeklyRaw) {
    // Integer dayOfWeek = ((Number) row[0]).intValue(); // 1=Sun, 2=Mon...
    // String status = row[1].toString();
    // Long count = (Long) row[2];

    // WeeklyComplaintChart w = dayMap.get(dayOfWeek);
    // if (Complaint.Status.valueOf(status) == Complaint.Status.OPEN) {
    // w.setOpen(count.intValue());
    // } else if (status.equals("CLOSED")) {
    // w.setClosed(count.intValue());
    // } else if (status.equals("IN_PROGRESS")) {
    // w.setInProgress(count.intValue());
    // } else if (status.equals("PENDING")) {
    // w.setPending(count.intValue());
    // }
    // }

    // dto.setWeeklyData(new ArrayList<>(dayMap.values()));

    // // 3. Engineer Stats
    // List<Object[]> inProgress =
    // dashboardRepository.countComplaintsByAssigneeAndStatus(Complaint.Status.IN_PROGRESS);
    // List<Object[]> pending =
    // dashboardRepository.countComplaintsByAssigneeAndStatus(Complaint.Status.PENDING);

    // Map<String, EngineerPerformanceChart> engineerMap = new HashMap<>();

    // // For IN_PROGRESS
    // List<Object[]> inProgressList =
    // dashboardRepository.countComplaintsByAssigneeAndStatus(Complaint.Status.IN_PROGRESS);
    // for (Object[] row : inProgressList) {
    // String name = (String) row[0];
    // Long count = (Long) row[1]; // ← index 1, not 2

    // engineerMap.computeIfAbsent(name, k -> new EngineerPerformanceChart())
    // .setInProgress(count.intValue())
    // .setName(name);
    // }

    // // For PENDING
    // List<Object[]> pendingList =
    // dashboardRepository.countComplaintsByAssigneeAndStatus(Complaint.Status.PENDING);
    // for (Object[] row : pendingList) {
    // String name = (String) row[0];
    // Long count = (Long) row[1]; // ← index 1

    // engineerMap.computeIfAbsent(name, k -> new EngineerPerformanceChart())
    // .setPending(count.intValue())
    // .setName(name);
    // }

    // dto.setEngineerStats(new ArrayList<>(engineerMap.values()));

    // // 5. Equipment Chart (example: total complaints per equipment)
    // // You can expand this later
    // dto.getEquipmentChartData().put("Open", Arrays.asList(256, 230, 245, 287,
    // 240, 250, 230, 295, 331, 431, 456, 521));
    // dto.getEquipmentChartData().put("In Progress", Arrays.asList(542, 480, 430,
    // 550, 530, 453, 380, 434, 568, 610, 700, 900));
    // dto.getEquipmentChartData().put("Pending", Arrays.asList(1200, 1300, 1250,
    // 1400, 1350, 1450, 1500, 1600, 1700, 1800, 1900, 2000));
    // dto.getEquipmentChartData().put("Closed", Arrays.asList(154, 184, 175, 203,
    // 210, 231, 240, 278, 252, 312, 320, 374));

    // return dto;
    // }
}