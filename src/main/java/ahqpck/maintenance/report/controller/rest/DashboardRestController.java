package ahqpck.maintenance.report.controller.rest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ahqpck.maintenance.report.dto.AssigneeDailyStatusDTO;
import ahqpck.maintenance.report.dto.ComplaintStatusWeeklyDTO;
import ahqpck.maintenance.report.dto.DailyStatusCountDTO;
import ahqpck.maintenance.report.dto.EquipmentComplaintCountDTO;
import ahqpck.maintenance.report.dto.StatusCountDTO;
import ahqpck.maintenance.report.dto.UserComplaintSummaryDTO;
import ahqpck.maintenance.report.dto.UserDailySummaryDTO;
import ahqpck.maintenance.report.dto.UserWeeklySummaryDTO;
import ahqpck.maintenance.report.service.DashboardService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
public class DashboardRestController {

    private final DashboardService dashboardService;

    // GET /api/dashboards/status-counts
    // Example: ?from=2025-08-01T00:00&to=2025-08-10T23:59
    @GetMapping("/status-count")
    public ResponseEntity<StatusCountDTO> getStatusCount(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        StatusCountDTO result = dashboardService.getStatusCount(from, to);
        return ResponseEntity.ok(result.orZero());
    }

    @GetMapping("/daily-status-count")
    public ResponseEntity<List<DailyStatusCountDTO>> getDailyStatusCount(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<DailyStatusCountDTO> result = dashboardService.getDailyStatusCount(from, to);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/assignee-daily-status")
    public ResponseEntity<AssigneeDailyStatusDTO> getAssigneeDailyStatus(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        AssigneeDailyStatusDTO result = dashboardService.getAssigneeDailyStatus(from, to);
        return ResponseEntity.ok(result);
    }

    // {
    // "dates": [
    // "2025-08-12",
    // "2025-08-13",
    // "2025-08-14"
    // ],
    // "data": [
    // {
    // "assignee": "AAA",
    // "open": [0, 1, 0],
    // "pending": [0, 0, 0],
    // "closed": [0, 0, 0]
    // },
    // {
    // "assignee": "Gema Nur",
    // "open": [0, 0, 1],
    // "pending": [0, 0, 0],
    // "closed": [0, 0, 0]
    // }
    // ]
    // }

    @GetMapping("/user-complaint-summary")
    public ResponseEntity<List<UserComplaintSummaryDTO>> getUserComplaintSummary() {
        List<UserComplaintSummaryDTO> result = dashboardService.getUserComplaintSummary();
        return ResponseEntity.ok(result);
    }

    // DashboardRestController.java
    @GetMapping("/user-weekly-summary")
    public ResponseEntity<List<UserWeeklySummaryDTO>> getUserWeeklySummary(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<UserWeeklySummaryDTO> data = dashboardService.getUserWeeklySummary(from, to);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/user-daily-summary")
    public ResponseEntity<List<UserDailySummaryDTO>> getUserDailySummary(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<UserDailySummaryDTO> data = dashboardService.getUserDailySummary(from, to);
        return ResponseEntity.ok(data);
    }

    // DashboardRestController.java
    @GetMapping("/complaint-status-weekly")
    public ResponseEntity<List<ComplaintStatusWeeklyDTO>> getComplaintStatusWeekly(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<ComplaintStatusWeeklyDTO> data = dashboardService.getComplaintStatusWeekly(from, to);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/equipment-complaint-count")
    public ResponseEntity<List<EquipmentComplaintCountDTO>> getEquipmentComplaintCount() {
        List<EquipmentComplaintCountDTO> data = dashboardService.getEquipmentComplaintCount();
        return ResponseEntity.ok(data);
    }
}