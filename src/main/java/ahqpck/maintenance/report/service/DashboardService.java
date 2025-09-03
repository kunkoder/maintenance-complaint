package ahqpck.maintenance.report.service;

import ahqpck.maintenance.report.dto.AssigneeDailyStatusDTO;
import ahqpck.maintenance.report.dto.AssigneeDailyStatusDetailDTO;
import ahqpck.maintenance.report.dto.DailyBreakdownDTO;
import ahqpck.maintenance.report.dto.DailyComplaintDTO;
import ahqpck.maintenance.report.dto.DailyWorkReportDTO;
import ahqpck.maintenance.report.dto.DailyWorkReportEquipmentDTO;
import ahqpck.maintenance.report.dto.EquipmentComplaintCountDTO;
import ahqpck.maintenance.report.dto.EquipmentWorkReportDTO;
import ahqpck.maintenance.report.dto.MonthlyBreakdownDTO;
import ahqpck.maintenance.report.dto.MonthlyComplaintDTO;
import ahqpck.maintenance.report.dto.MonthlyWorkReportDTO;
import ahqpck.maintenance.report.dto.MonthlyWorkReportEquipmentDTO;
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

    public List<DailyComplaintDTO> getDailyComplaint(LocalDateTime from, LocalDateTime to) {
        // Default: last 7 days (today + 6 previous days)
        LocalDateTime defaultTo = LocalDateTime.now().with(LocalTime.MAX); // 23:59:59.999
        LocalDateTime defaultFrom = defaultTo.minusDays(6).with(LocalTime.MIN); // 00:00:00.000

        LocalDateTime effectiveFrom = from != null ? from : defaultFrom;
        LocalDateTime effectiveTo = to != null ? to : defaultTo;

        // Ensure from <= to
        // if (effectiveFrom.isAfter(effectiveTo)) {
        //     throw new IllegalArgumentException("Invalid date range: 'from' must be before or equal to 'to'");
        // }

        return dashboardRepository.getDailyComplaint(effectiveFrom, effectiveTo);
    }

    public List<MonthlyComplaintDTO> getMonthlyComplaint(Integer year) {
        
        Integer effectiveYear = (year != null && year > 1900) ? year : LocalDate.now().getYear();
        return dashboardRepository.getMonthlyComplaint(effectiveYear);
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

    public List<DailyBreakdownDTO> getDailyBreakdownTime(LocalDate from, LocalDate to) {
        LocalDate defaultTo = LocalDate.now();
        LocalDate defaultFrom = defaultTo.minusDays(6); // last 7 days

        LocalDate effectiveFrom = from != null ? from : defaultFrom;
        LocalDate effectiveTo = to != null ? to : defaultTo;

        return dashboardRepository.getDailyBreakdownTime(effectiveFrom, effectiveTo);
    }

    public List<MonthlyBreakdownDTO> getMonthlyBreakdownTime(Integer year) {
        Integer effectiveYear = (year != null && year > 1900) ? year : LocalDate.now().getYear();
        return dashboardRepository.getMonthlyBreakdownTime(effectiveYear);
    }

    public List<EquipmentWorkReportDTO> getEquipmentWorkReport() {
        return dashboardRepository.getEquipmentWorkReport();
    }

    public List<DailyWorkReportDTO> getDailyWorkReport(LocalDate from, LocalDate to) {
        LocalDate defaultTo = LocalDate.now();
        LocalDate defaultFrom = defaultTo.minusDays(6); // last 7 days

        LocalDate effectiveFrom = from != null ? from : defaultFrom;
        LocalDate effectiveTo = to != null ? to : defaultTo;

        return dashboardRepository.getDailyWorkReport(effectiveFrom, effectiveTo);
    }

    // === Monthly Work Report Count ===
    public List<MonthlyWorkReportDTO> getMonthlyWorkReport(Integer year) {
        Integer effectiveYear = (year != null && year > 1900) ? year : LocalDate.now().getYear();
        return dashboardRepository.getMonthlyWorkReport(effectiveYear);
    }

    public List<DailyWorkReportEquipmentDTO> getDailyWorkReportEquipment(LocalDate from, LocalDate to, String equipmentCode) {
        LocalDate defaultTo = LocalDate.now();
        LocalDate defaultFrom = defaultTo.minusDays(6); // Last 7 days

        LocalDate effectiveFrom = from != null ? from : defaultFrom;
        LocalDate effectiveTo = to != null ? to : defaultTo;

        String effectiveEquipmentCode = (equipmentCode != null && !equipmentCode.trim().isEmpty())
                ? equipmentCode.trim()
                : null;

        return dashboardRepository.getDailyWorkReportEquipment(effectiveFrom, effectiveTo, effectiveEquipmentCode);
    }

    public List<MonthlyWorkReportEquipmentDTO> getMonthlyWorkReportEquipment(Integer year, String equipmentCode) {
        Integer effectiveYear = (year != null && year > 1900) ? year : LocalDate.now().getYear();
        return dashboardRepository.getMonthlyWorkReportEquipment(effectiveYear, equipmentCode);
    }
}