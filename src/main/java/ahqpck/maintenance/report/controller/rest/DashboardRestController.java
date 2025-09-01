package ahqpck.maintenance.report.controller.rest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ahqpck.maintenance.report.dto.AssigneeDailyStatusDTO;
import ahqpck.maintenance.report.dto.DailyBreakdownDTO;
import ahqpck.maintenance.report.dto.DailyStatusCountDTO;
import ahqpck.maintenance.report.dto.EquipmentComplaintCountDTO;
import ahqpck.maintenance.report.dto.MonthlyBreakdownDTO;
import ahqpck.maintenance.report.dto.MonthlyStatusCountDTO;
import ahqpck.maintenance.report.dto.StatusCountDTO;
import ahqpck.maintenance.report.service.DashboardService;
import ahqpck.maintenance.report.util.EmailUtil;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
public class DashboardRestController {

    private final DashboardService dashboardService;

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

    @GetMapping("/monthly-status-count")
    public ResponseEntity<List<MonthlyStatusCountDTO>> getMonthlyStatusCount(
            @RequestParam(name = "year", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime year) {

        List<MonthlyStatusCountDTO> result = dashboardService.getMonthlyStatusCount(year);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/assignee-daily-status")
    public ResponseEntity<AssigneeDailyStatusDTO> getAssigneeDailyStatus(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        AssigneeDailyStatusDTO result = dashboardService.getAssigneeDailyStatus(from, to);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/equipment-complaint-count")
    public ResponseEntity<List<EquipmentComplaintCountDTO>> getEquipmentComplaintCount() {
        List<EquipmentComplaintCountDTO> data = dashboardService.getEquipmentComplaintCount();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/daily-breakdown")
    public ResponseEntity<List<DailyBreakdownDTO>> getDailyBreakdown(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<DailyBreakdownDTO> result = dashboardService.getDailyBreakdownTime(from, to);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/monthly-breakdown")
    public ResponseEntity<List<MonthlyBreakdownDTO>> getMonthlyBreakdown(
            @RequestParam(name = "year", required = false) Integer year) {

        List<MonthlyBreakdownDTO> result = dashboardService.getMonthlyBreakdownTime(year);
        return ResponseEntity.ok(result);
    }

    // private final EmailUtil emailUtil;

    // @GetMapping("/test-email")
    // public String testEmail() {
    //     try {
    //         emailUtil.sendAccountActivationEmail("laught.coffin@gmail.com", "test-token-12345");;
    //         return "Email sent!";
    //     } catch (Exception e) {
    //         return "Failed: " + e.getMessage();
    //     }
    // }
}