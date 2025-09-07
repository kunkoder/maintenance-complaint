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
import ahqpck.maintenance.report.dto.DailyComplaintDTO;
import ahqpck.maintenance.report.dto.DailyWorkReportDTO;
import ahqpck.maintenance.report.dto.DailyWorkReportEquipmentDTO;
import ahqpck.maintenance.report.dto.EquipmentComplaintCountDTO;
import ahqpck.maintenance.report.dto.EquipmentCountDTO;
import ahqpck.maintenance.report.dto.EquipmentWorkReportDTO;
import ahqpck.maintenance.report.dto.MonthlyBreakdownDTO;
import ahqpck.maintenance.report.dto.MonthlyComplaintDTO;
import ahqpck.maintenance.report.dto.MonthlyWorkReportDTO;
import ahqpck.maintenance.report.dto.MonthlyWorkReportEquipmentDTO;
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

    @GetMapping("/daily-complaint")
    public ResponseEntity<List<DailyComplaintDTO>> getDailyComplaint(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {

        List<DailyComplaintDTO> result = dashboardService.getDailyComplaint(from, to);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/monthly-complaint")
    public ResponseEntity<List<MonthlyComplaintDTO>> getMonthlyComplaint(
            @RequestParam(name = "year", required = false) Integer year) {

        List<MonthlyComplaintDTO> result = dashboardService.getMonthlyComplaint(year);
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

    @GetMapping("/equipment-work-report")
    public ResponseEntity<List<EquipmentWorkReportDTO>> getEquipmentWorkReport() {
        List<EquipmentWorkReportDTO> data = dashboardService.getEquipmentWorkReport();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/daily-work-report")
    public ResponseEntity<List<DailyWorkReportDTO>> getDailyWorkReport(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<DailyWorkReportDTO> result = dashboardService.getDailyWorkReport(from, to);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/monthly-work-report")
    public ResponseEntity<List<MonthlyWorkReportDTO>> getMonthlyWorkReport(
            @RequestParam(name = "year", required = false) Integer year) {

        List<MonthlyWorkReportDTO> result = dashboardService.getMonthlyWorkReport(year);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/daily-work-report-equipment")
    public ResponseEntity<List<DailyWorkReportEquipmentDTO>> getDailyWorkReportEquipment(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "equipmentCode", required = false) String equipmentCode) {

        List<DailyWorkReportEquipmentDTO> result = dashboardService.getDailyWorkReportEquipment(from, to, equipmentCode);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/monthly-work-report-equipment")
    public ResponseEntity<List<MonthlyWorkReportEquipmentDTO>> getMonthlyWorkReportEquipment(
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "equipmentCode", required = false) String equipmentCode) {

        List<MonthlyWorkReportEquipmentDTO> result = dashboardService.getMonthlyWorkReportEquipment(year, equipmentCode);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/equipment-count")
    public ResponseEntity<List<EquipmentCountDTO>> getEquipmentCount() {
        List<EquipmentCountDTO> data = dashboardService.getEquipmentCount();
        return ResponseEntity.ok(data);
    }

    // private final EmailUtil emailUtil;

    // @GetMapping("/test-email")
    // public String testEmail() {
    // try {
    // emailUtil.sendAccountActivationEmail("laught.coffin@gmail.com",
    // "test-token-12345");;
    // return "Email sent!";
    // } catch (Exception e) {
    // return "Failed: " + e.getMessage();
    // }
    // }
}