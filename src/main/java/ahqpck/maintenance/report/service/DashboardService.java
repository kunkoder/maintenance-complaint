package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.AssigneeDailyStatusDTO;
import ahqpck.maintenance.report.dto.AssigneeDailyStatusDetailDTO;
import ahqpck.maintenance.report.dto.DailyStatusCountDTO;
import ahqpck.maintenance.report.dto.EquipmentComplaintCountDTO;
import ahqpck.maintenance.report.dto.MonthlyStatusCountDTO;
import ahqpck.maintenance.report.dto.StatusCountDTO;
import ahqpck.maintenance.report.repository.DashboardRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;

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

    public List<MonthlyStatusCountDTO> getMonthlyStatusCount(LocalDateTime year) {
        // If null, default to current year
        LocalDateTime effectiveYear = (year != null) ? year : LocalDateTime.now();

        return dashboardRepository.getMonthlyStatusCount(effectiveYear);
    }

    public AssigneeDailyStatusDTO getAssigneeDailyStatus(LocalDateTime from, LocalDateTime to) {

        if (from == null || to == null) {
            to = LocalDateTime.now().with(LocalTime.MAX);
            from = to.minusDays(2);
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

        Map<String, AssigneeDailyStatusDetailDTO> assigneeMap = new LinkedHashMap<>();

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

        for (Object[] row : results) {
            String assignee = (String) row[0];
            String status = (String) row[1];
            LocalDate reportDate = ((java.sql.Date) row[2]).toLocalDate();
            Long count = ((Number) row[3]).longValue();

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

        AssigneeDailyStatusDTO response = new AssigneeDailyStatusDTO();
        response.setDates(dateList.stream().map(LocalDate::toString).collect(Collectors.toList()));
        response.setData(new ArrayList<>(assigneeMap.values()));

        return response;
    }

    public List<EquipmentComplaintCountDTO> getEquipmentComplaintCount() {
        return dashboardRepository.getEquipmentComplaintCount();
    }
}