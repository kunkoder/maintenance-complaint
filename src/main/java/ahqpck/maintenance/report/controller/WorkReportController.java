package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.dto.AreaDTO;
import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.dto.UserDTO;
import ahqpck.maintenance.report.dto.WorkReportDTO;
import ahqpck.maintenance.report.service.AreaService;
import ahqpck.maintenance.report.service.EquipmentService;
import ahqpck.maintenance.report.service.UserService;
import ahqpck.maintenance.report.service.WorkReportService;
import ahqpck.maintenance.report.util.ImportUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/work-reports")
@RequiredArgsConstructor
public class WorkReportController {

    private final WorkReportService workReportService;
    private final AreaService areaService;
    private final EquipmentService equipmentService;
    private final UserService userService;

    // === LIST WORK REPORTS ===
    @GetMapping
    public String listWorkReports(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "reportDate") String sortBy,
            @RequestParam(defaultValue = "false") boolean asc,
            Model model) {

        try {
            int zeroBasedPage = page - 1;
            Page<WorkReportDTO> reportPage = workReportService.getAllWorkReports(keyword, zeroBasedPage, size, sortBy,
                    asc);

            model.addAttribute("workReports", reportPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);

            model.addAttribute("title", "Work Report");
            model.addAttribute("sortFields", new String[] {
                    "reportDate",
                    "shift",
                    "category",
                    "status",
                    "startTime",
                    "stopTime",
                    "equipment.name",
                    "area.name",
                    "technician.name",
                    "supervisor.name",
                    "problem",
                    "solution",
                    "workType",
                    "remark"
            });

            // Load dropdown data
            List<UserDTO> users = userService.getAllUsers(null, 0, Integer.MAX_VALUE, "name", true)
                    .getContent().stream().collect(Collectors.toList());
            model.addAttribute("users", users);

            List<AreaDTO> areas = areaService.getAllAreas(null, 0, Integer.MAX_VALUE, "name", true)
                    .getContent().stream().collect(Collectors.toList());
            model.addAttribute("areas", areas);

            List<EquipmentDTO> equipments = equipmentService.getAllEquipments(null, 0, Integer.MAX_VALUE, "name", true)
                    .getContent().stream().collect(Collectors.toList());
            model.addAttribute("equipments", equipments);

            // Empty DTO for create form
            model.addAttribute("workReportDTO", new WorkReportDTO());

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load work reports: " + e.getMessage());
            e.printStackTrace();
        }

        return "work-report/index";
    }

    // === CREATE WORK REPORT ===
    @PostMapping
    public String createWorkReport(
            @Valid @ModelAttribute WorkReportDTO workReportDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {

                System.out.println("Work Report DTO: " + workReportDTO);

        if (bindingResult.hasErrors()) {
            handleBindingErrors(bindingResult, ra, workReportDTO);
            return "redirect:/work-reports";
        }

        try {
            System.out.println(workReportDTO);
            workReportService.createWorkReport(workReportDTO);
            ra.addFlashAttribute("success", "Work report created successfully.");
            return "redirect:/work-reports";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to create work report: " + e.getMessage());
            ra.addFlashAttribute("workReportDTO", workReportDTO);
            return "redirect:/work-reports";
        }
    }

    // === UPDATE WORK REPORT ===
    @PostMapping("/update")
    public String updateWorkReport(
            @Valid @ModelAttribute WorkReportDTO workReportDTO,
            BindingResult bindingResult,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            handleBindingErrors(bindingResult, ra, workReportDTO);
            return "redirect:/work-reports";
        }

        try {
            workReportService.updateWorkReport(workReportDTO);
            ra.addFlashAttribute("success", "Work report updated successfully.");
            return "redirect:/work-reports";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to update work report: " + e.getMessage());
            ra.addFlashAttribute("workReportDTO", workReportDTO);
            return "redirect:/work-reports";
        }
    }

    // === DELETE WORK REPORT ===
    @GetMapping("/delete/{id}")
    public String deleteWorkReport(@PathVariable String id, RedirectAttributes ra) {
        try {
            workReportService.deleteWorkReport(id);
            ra.addFlashAttribute("success", "Work report deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to delete work report: " + e.getMessage());
        }
        return "redirect:/work-reports";
    }

    @PostMapping("/import")
    public String importWorkReports(
            @RequestParam("data") String dataJson,
            @RequestParam(value = "sheet", required = false) String sheet,
            @RequestParam(value = "headerRow", required = false) Integer headerRow,
            RedirectAttributes ra) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> data = mapper.readValue(dataJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            // ra.addFlashAttribute("error", data);

            ImportUtil.ImportResult result = workReportService.importWorkReportsFromExcel(data);

            if (result.getImportedCount() > 0 && !result.hasErrors()) {
                ra.addFlashAttribute("success",
                        "Successfully imported " + result.getImportedCount() + " work report record(s).");
            } else if (result.getImportedCount() > 0) {
                StringBuilder msg = new StringBuilder("Imported ").append(result.getImportedCount())
                        .append(" record(s), but ").append(result.getErrorMessages().size()).append(" error(s):");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            } else {
                StringBuilder msg = new StringBuilder("Failed to import any work report:");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            }

            return "redirect:/work-reports";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Bulk import failed: " + e.getMessage());
            return "redirect:/WorkReports";
        }
    }

    // === HELPERS ===

    private void handleBindingErrors(BindingResult bindingResult, RedirectAttributes ra, WorkReportDTO dto) {
        String errorMessage = bindingResult.getAllErrors().stream()
                .map(error -> {
                    String field = (error instanceof FieldError)
                            ? ((FieldError) error).getField()
                            : "Input";
                    String message = error.getDefaultMessage();
                    return field + ": " + message;
                })
                .collect(Collectors.joining(" | "));

        ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
        ra.addFlashAttribute("workReportDTO", dto);
    }
}