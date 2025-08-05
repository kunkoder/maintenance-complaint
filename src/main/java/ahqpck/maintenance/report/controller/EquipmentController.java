// src/main/java/ahqpck/maintenance/report/controller/EquipmentController.java
package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.exception.ImportException;
import ahqpck.maintenance.report.exception.ValidationException;
import ahqpck.maintenance.report.service.EquipmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ValidationUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/equipments")
@RequiredArgsConstructor
public class EquipmentController {

    @Autowired
    private org.springframework.validation.Validator globalValidator; // Spring's Validator

    private final EquipmentService equipmentService;

    @Value("${app.upload-equipment-image.dir:src/main/resources/static/upload/equipment/image}")
    private String uploadDir;

    @GetMapping
    public String listEquipments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "true") boolean asc,
            Model model) {

        try {
            var equipmentPage = equipmentService.getAllEquipments(keyword, page, size, sortBy, asc);
            model.addAttribute("equipments", equipmentPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentPage", page);
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);

            model.addAttribute("title", "Equipments");
            model.addAttribute("sortFields", new String[] {
                    "code", "name", "model", "manufacturer", "serialNo", "qty", "capacity", "manufacturedDate",
                    "commissionedDate"
            });
            model.addAttribute("equipmentDTO", new EquipmentDTO());

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load equipment: " + e.getMessage());
        }

        return "equipment/index";
    }

    @PostMapping
    public String createEquipment(
            @Valid @ModelAttribute EquipmentDTO equipmentDTO,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            ra.addFlashAttribute("equipmentDTO", equipmentDTO);
            return "redirect:/equipments";
        }

        try {
            equipmentService.createEquipment(equipmentDTO, imageFile);
            ra.addFlashAttribute("success", "Equipment created successfully.");
            return "redirect:/equipments";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("equipmentDTO", equipmentDTO);
            return "redirect:/equipments";
        }
    }

    @PostMapping("/update")
    public String updateEquipment(
            @Valid @ModelAttribute EquipmentDTO equipmentDTO,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "deleteImage", required = false, defaultValue = "false") boolean deleteImage,
            RedirectAttributes ra) {

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                    .map(error -> {
                        String field = (error instanceof FieldError) ? ((FieldError) error).getField() : "Input";
                        String message = error.getDefaultMessage();
                        return field + ": " + message;
                    })
                    .collect(Collectors.joining(" | "));

            ra.addFlashAttribute("error", errorMessage.isEmpty() ? "Invalid input" : errorMessage);
            ra.addFlashAttribute("equipmentDTO", equipmentDTO);
            return "redirect:/equipments";
        }

        try {
            equipmentService.updateEquipment(equipmentDTO, imageFile, deleteImage);
            ra.addFlashAttribute("success", "Equipment updated successfully.");
            return "redirect:/equipments";

        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("equipmentDTO", equipmentDTO);
            return "redirect:/equipments";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteEquipment(@PathVariable String id, RedirectAttributes ra) {
        try {
            equipmentService.deleteEquipment(id);
            ra.addFlashAttribute("success", "Equipment deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/equipments";
    }

    @PostMapping("/import")
public String importEquipments(@RequestBody Map<String, Object> payload,
                               RedirectAttributes ra) {
    try {
        List<Map<String, Object>> data = (List<Map<String, Object>>) payload.get("data");
        String sheet = (String) payload.get("sheet");
        Integer headerRow = (Integer) payload.get("headerRow");

        EquipmentService.ImportResult result = equipmentService.importEquipmentsFromExcel(data);
        System.out.println(result.getImportedCount());

        if (result.getImportedCount() > 0 && !result.hasErrors()) {
            ra.addFlashAttribute("success", "Successfully imported " + result.getImportedCount() + " equipment record(s).");
        } else if (result.getImportedCount() > 0) {
            // Combine warning + errors into `error` as pipe-separated string
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("Imported ").append(result.getImportedCount())
                    .append(" record(s), but ").append(result.getErrorMessages().size())
                    .append(" row(s) had errors:");
            for (String err : result.getErrorMessages()) {
                errorMsg.append("|").append(err);
            }
            ra.addFlashAttribute("error", errorMsg.toString());
        } else {
            // All failed
            StringBuilder errorMsg = new StringBuilder("Failed to import any equipment:");
            for (String err : result.getErrorMessages()) {
                errorMsg.append("|").append(err);
            }
            ra.addFlashAttribute("error", errorMsg.toString());
        }
        System.out.println(result.getErrorMessages());

        return "redirect:/equipments";

    } catch (Exception e) {
        ra.addFlashAttribute("error", "Bulk import failed: " + e.getMessage());
        return "redirect:/equipments";
    }
}

    private static String toString(Object obj) {
        return obj != null ? obj.toString().trim() : null;
    }

    private static Integer parseInteger(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty())
            return null;
        try {
            return Integer.parseInt(obj.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + obj);
        }
    }

    private static LocalDate toLocalDate(Object obj) {
        if (obj == null || obj.toString().trim().isEmpty())
            return null;

        String str = obj.toString().trim();

        // Handle Excel serial dates (e.g., 45359)
        if (str.matches("\\d+(\\.\\d+)?")) {
            double excelDate = Double.parseDouble(str);
            return convertExcelDate(excelDate);
        }

        // Define supported date formats
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("MM-dd-yyyy"));

        // Try each format
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(str, formatter);
            } catch (DateTimeParseException ignored) {
                // Try next format
            }
        }

        // If all fail
        throw new IllegalArgumentException("Unrecognized date format: " + str);
    }

    private static LocalDate convertExcelDate(double serialDate) {
        int n = (int) serialDate;
        if (n >= 60)
            n--; // Adjust for Excel's 1900 leap year bug
        return LocalDate.of(1899, 12, 30).plusDays(n);
    }

}