package ahqpck.maintenance.report.controller;

import ahqpck.maintenance.report.dto.EquipmentDTO;
import ahqpck.maintenance.report.service.EquipmentService;
import ahqpck.maintenance.report.util.ImportUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/equipments")
@RequiredArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @Value("${app.upload-equipment-image.dir:src/main/resources/static/upload/equipment/image}")
    private String uploadDir;

    @GetMapping
    public String listEquipments(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page, // Now starts at 1
            @RequestParam(defaultValue = "10") @Min(1) int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "true") boolean asc,
            Model model) {

        try {
            // Convert 1-based page to 0-based index (Spring expects 0 = first page)
            int zeroBasedPage = page - 1;

            var equipmentPage = equipmentService.getAllEquipments(keyword, zeroBasedPage, size, sortBy, asc);
            model.addAttribute("equipments", equipmentPage);
            model.addAttribute("keyword", keyword);
            model.addAttribute("currentPage", page); // Store 1-based for Thymeleaf
            model.addAttribute("pageSize", size);
            model.addAttribute("sortBy", sortBy);
            model.addAttribute("asc", asc);

            model.addAttribute("title", "Equipments");
            model.addAttribute("sortFields", new String[] {
                    "code", "name", "model", "manufacturer", "serialNo", "qty", "capacity", "manufacturedDate",
                    "commissionedDate"
            });
            model.addAttribute("equipmentDTO", new EquipmentDTO());
            System.out.println(model);

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
    public String importEquipments(
            @RequestParam("data") String dataJson,
            @RequestParam(value = "sheet", required = false) String sheet,
            @RequestParam(value = "headerRow", required = false) Integer headerRow,
            RedirectAttributes ra) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> data = mapper.readValue(dataJson,
                    new TypeReference<List<Map<String, Object>>>() {
                    });

            ImportUtil.ImportResult result = equipmentService.importEquipmentsFromExcel(data);

            if (result.getImportedCount() > 0 && !result.hasErrors()) {
                ra.addFlashAttribute("success",
                        "Successfully imported " + result.getImportedCount() + " equipment record(s).");
            } else if (result.getImportedCount() > 0) {
                StringBuilder msg = new StringBuilder("Imported ").append(result.getImportedCount())
                        .append(" record(s), but ").append(result.getErrorMessages().size()).append(" error(s):");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            } else {
                StringBuilder msg = new StringBuilder("Failed to import any equipment:");
                for (String err : result.getErrorMessages()) {
                    msg.append("|").append(err);
                }
                ra.addFlashAttribute("error", msg.toString());
            }

            return "redirect:/equipments";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Bulk import failed: " + e.getMessage());
            return "redirect:/equipments";
        }
    }
}